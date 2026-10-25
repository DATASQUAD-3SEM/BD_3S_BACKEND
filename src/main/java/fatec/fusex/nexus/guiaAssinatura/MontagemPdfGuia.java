package fatec.fusex.nexus.guiaAssinatura;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;

/**
 * Monta o PDF da Guia com as assinaturas ja registradas aplicadas nas linhas do medico e do chefe.
 *
 * E usado por todos os endpoints que entregam o PDF da Guia (medico e chefe, beneficiario e recepcao),
 * para que o documento seja sempre o mesmo. O arquivo original anexado pelo Setor de Guias nao e alterado:
 * o carimbo e aplicado em memoria a cada pedido. Deve ser chamado dentro de uma transacao, pois le o
 * usuario de cada assinatura.
 */
@Component
public class MontagemPdfGuia {

    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final CarimboAssinaturaPdf carimboAssinaturaPdf;

    public MontagemPdfGuia(AssinaturaGuiaRepository assinaturaGuiaRepository,
                           ArmazenamentoArquivo armazenamentoArquivo,
                           CarimboAssinaturaPdf carimboAssinaturaPdf) {
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.carimboAssinaturaPdf = carimboAssinaturaPdf;
    }

    /** Devolve os bytes do PDF da Guia; sem nenhuma assinatura, devolve o PDF original. */
    public byte[] montar(Guia guia) {
        Map<AssinaturaGuiaPapel, byte[]> assinaturas = new EnumMap<>(AssinaturaGuiaPapel.class);
        for (AssinaturaGuia assinatura : assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(guia.getIdGuia()))) {
            String caminhoImagem = assinatura.getUsuario().getAssinaturaUrl();
            if (caminhoImagem != null) {
                assinaturas.put(assinatura.getPapel(), lerArquivo(caminhoImagem));
            }
        }

        byte[] original = lerArquivo(guia.getArquivoUrl());
        return assinaturas.isEmpty() ? original : carimboAssinaturaPdf.aplicar(original, assinaturas);
    }

    private byte[] lerArquivo(String caminho) {
        try (InputStream entrada = armazenamentoArquivo.carregar(caminho).getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo " + caminho, e);
        }
    }
}
