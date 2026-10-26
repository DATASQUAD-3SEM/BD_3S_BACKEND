package fatec.fusex.nexus.recepcao;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.MontagemPdfGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;

/**
 * Entrega o PDF da Guia a recepcao (T6-BE2). Somente Guias de pre-guias Concluidas sao liberadas (403 nos demais casos).
 */
@Service
public class RecepcaoGuiaPdfService {

    private final GuiaRepository guiaRepository;
    private final UsuarioLogado usuarioLogado;
    private final MontagemPdfGuia montagemPdfGuia;

    public RecepcaoGuiaPdfService(GuiaRepository guiaRepository,
                                  UsuarioLogado usuarioLogado,
                                  MontagemPdfGuia montagemPdfGuia) {
        this.guiaRepository = guiaRepository;
        this.usuarioLogado = usuarioLogado;
        this.montagemPdfGuia = montagemPdfGuia;
    }

    @Transactional(readOnly = true)
    public PdfGuiaRecepcao pdfDaGuia(Long guiaId, String modo) {
        usuarioLogado.exigirPerfil(PerfilUsuario.RECEPCAO);
        ModoPdfGuia modoEscolhido = ModoPdfGuia.de(modo);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + guiaId));

        if (guia.getPreGuia().getStatus() != PreGuiaStatus.CONCLUIDA) {
            throw new AcessoNegadoException(
                    "Guia indisponivel: a pre-guia ainda nao esta concluida com as duas assinaturas");
        }

        // O PDF entregue a recepcao ja sai com as assinaturas do medico e do chefe
        ByteArrayResource arquivo = new ByteArrayResource(montagemPdfGuia.montar(guia));
        return new PdfGuiaRecepcao(arquivo, "guia-" + guia.getNumero() + ".pdf", modoEscolhido.isAnexo());
    }
}