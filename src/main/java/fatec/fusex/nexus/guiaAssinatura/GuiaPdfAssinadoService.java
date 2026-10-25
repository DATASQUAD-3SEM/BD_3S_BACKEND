package fatec.fusex.nexus.guiaAssinatura;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.dto.PdfGuiaAssinada;
import fatec.fusex.nexus.minhasPreGuias.ModoDownload;
import fatec.fusex.nexus.usuario.PerfilUsuario;

/**
 * Monta o PDF da Guia com as assinaturas ja feitas (T5-BE2). Acesso restrito a medicos e ao chefe.
 */
@Service
public class GuiaPdfAssinadoService {

    private final GuiaRepository guiaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final UsuarioLogado usuarioLogado;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final CarimboAssinaturaPdf carimboAssinaturaPdf;

    public GuiaPdfAssinadoService(GuiaRepository guiaRepository,
                                  AssinaturaGuiaRepository assinaturaGuiaRepository,
                                  UsuarioLogado usuarioLogado,
                                  ArmazenamentoArquivo armazenamentoArquivo,
                                  CarimboAssinaturaPdf carimboAssinaturaPdf) {
        this.guiaRepository = guiaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.usuarioLogado = usuarioLogado;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.carimboAssinaturaPdf = carimboAssinaturaPdf;
    }

    @Transactional(readOnly = true)
    public PdfGuiaAssinada pdfDaGuia(Long guiaId, String modo) {
        usuarioLogado.exigirPerfil(PerfilUsuario.MEDICO, PerfilUsuario.CHEFE_FUSEX);
        ModoDownload modoEscolhido = ModoDownload.from(modo);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + guiaId));

        Map<AssinaturaGuiaPapel, byte[]> assinaturas = new EnumMap<>(AssinaturaGuiaPapel.class);
        for (AssinaturaGuia assinatura : assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(guiaId))) {
            String caminhoImagem = assinatura.getUsuario().getAssinaturaUrl();
            if (caminhoImagem != null) {
                assinaturas.put(assinatura.getPapel(), lerArquivo(caminhoImagem));
            }
        }

        byte[] original = lerArquivo(guia.getArquivoUrl());
        byte[] pdf = assinaturas.isEmpty()
                ? original
                : carimboAssinaturaPdf.aplicar(original, assinaturas);

        return new PdfGuiaAssinada(
                new ByteArrayResource(pdf),
                "guia-" + guia.getNumero() + ".pdf",
                modoEscolhido == ModoDownload.BAIXAR);
    }

    private byte[] lerArquivo(String caminho) {
        try (InputStream entrada = armazenamentoArquivo.carregar(caminho).getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo " + caminho, e);
        }
    }
}