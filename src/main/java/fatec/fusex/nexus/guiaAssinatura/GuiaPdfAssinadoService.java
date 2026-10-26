package fatec.fusex.nexus.guiaAssinatura;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
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
    private final UsuarioLogado usuarioLogado;
    private final MontagemPdfGuia montagemPdfGuia;

    public GuiaPdfAssinadoService(GuiaRepository guiaRepository,
                                  UsuarioLogado usuarioLogado,
                                  MontagemPdfGuia montagemPdfGuia) {
        this.guiaRepository = guiaRepository;
        this.usuarioLogado = usuarioLogado;
        this.montagemPdfGuia = montagemPdfGuia;
    }

    @Transactional(readOnly = true)
    public PdfGuiaAssinada pdfDaGuia(Long guiaId, String modo) {
        usuarioLogado.exigirPerfil(PerfilUsuario.MEDICO, PerfilUsuario.CHEFE_FUSEX);
        ModoDownload modoEscolhido = ModoDownload.from(modo);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + guiaId));

        return new PdfGuiaAssinada(
                new ByteArrayResource(montagemPdfGuia.montar(guia)),
                "guia-" + guia.getNumero() + ".pdf",
                modoEscolhido == ModoDownload.BAIXAR);
    }
}
