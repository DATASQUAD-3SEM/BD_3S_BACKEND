package fatec.fusex.nexus.guiaAssinatura;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;

/**
 * Monta o PDF da Guia com as assinaturas ja registradas aplicadas nas linhas do medico, do chefe e, quando a
 * Guia esta finalizada (T23-BE1), do beneficiario.
 *
 * E usado por todos os endpoints que entregam o PDF da Guia (medico e chefe, beneficiario e recepcao),
 * para que o documento seja sempre o mesmo. O arquivo original anexado pelo Setor de Guias nao e alterado:
 * o carimbo e aplicado em memoria a cada pedido. Deve ser chamado dentro de uma transacao, pois le o
 * usuario de cada assinatura e as sessoes.
 */
@Component
public class MontagemPdfGuia {

    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final CarimboAssinaturaPdf carimboAssinaturaPdf;
    private final SessaoRepository sessaoRepository;

    public MontagemPdfGuia(AssinaturaGuiaRepository assinaturaGuiaRepository,
                           ArmazenamentoArquivo armazenamentoArquivo,
                           CarimboAssinaturaPdf carimboAssinaturaPdf,
                           SessaoRepository sessaoRepository) {
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.carimboAssinaturaPdf = carimboAssinaturaPdf;
        this.sessaoRepository = sessaoRepository;
    }

    /** Devolve os bytes do PDF da Guia; sem nenhuma assinatura, devolve o PDF original. */
    public byte[] montar(Guia guia) {
        Map<AssinaturaGuiaPapel, byte[]> assinaturas = new EnumMap<>(AssinaturaGuiaPapel.class);
        for (AssinaturaGuia assinatura : assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(guia.getIdGuia()))) {
            String caminhoImagem = assinatura.getAssinatura().getAssinaturaUrl();
            assinaturas.put(assinatura.getPapel(), lerArquivo(caminhoImagem));
        }

        byte[] assinaturaResponsavel = assinaturaDoBeneficiario(guia).map(this::lerArquivo).orElse(null);

        byte[] original = lerArquivo(guia.getArquivoUrl());
        if (assinaturas.isEmpty() && assinaturaResponsavel == null) {
            return original;
        }
        return carimboAssinaturaPdf.aplicar(original, assinaturas, assinaturaResponsavel);
    }

    /**
     * T23-BE1: a assinatura do beneficiario entra quando a Guia esta finalizada, decidido pelos fatos (sessoes e
     * validade), e nao por Guia.situacaoExecucao: ao menos uma sessao ASSINADA e, alem disso, todas as sessoes
     * ASSINADAS (Realizada) ou a validade ja passou (Encerrada). A imagem e a da ultima sessao assinada.
     */
    private Optional<String> assinaturaDoBeneficiario(Guia guia) {
        List<Sessao> sessoes = sessaoRepository
                .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                        List.of(guia.getIdGuia()));

        boolean algumaAssinada = sessoes.stream().anyMatch(MontagemPdfGuia::assinada);
        boolean todasAssinadas = !sessoes.isEmpty() && sessoes.stream().allMatch(MontagemPdfGuia::assinada);
        boolean validadePassou = guia.getDataValidadeAte() != null
                && Beneficiario.hoje().isAfter(guia.getDataValidadeAte());

        if (!algumaAssinada || !(todasAssinadas || validadePassou)) {
            return Optional.empty();
        }

        return sessoes.stream()
                .filter(sessao -> assinada(sessao) && sessao.getAssinatura() != null)
                .max(Comparator.comparing(Sessao::getDataAssinatura, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(sessao -> sessao.getAssinatura().getAssinaturaUrl());
    }

    private static boolean assinada(Sessao sessao) {
        return sessao.getStatus() == SituacaoSessao.ASSINADA;
    }

    private byte[] lerArquivo(String caminho) {
        try (InputStream entrada = armazenamentoArquivo.carregar(caminho).getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo " + caminho, e);
        }
    }
}
