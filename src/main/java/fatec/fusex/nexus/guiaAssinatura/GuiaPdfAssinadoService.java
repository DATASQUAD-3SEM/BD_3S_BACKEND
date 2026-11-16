package fatec.fusex.nexus.guiaAssinatura;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.dto.PdfGuiaAssinada;
import fatec.fusex.nexus.minhasPreGuias.ModoDownload;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Monta o PDF da Guia com as assinaturas ja feitas (T5-BE2 e T23-BE2).
 *
 * Medico e chefe: PDF para download ou visualizacao, com {@code modo} obrigatorio (como antes).
 * Beneficiario dono da Guia: sempre inline, sem {@code modo}; as mesmas regras de entrega da T1-BE2
 * (pre-guia concluida e duas assinaturas). Guia de outro beneficiario: 403.
 */
@Service
public class GuiaPdfAssinadoService {

    private final GuiaRepository guiaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final UsuarioLogado usuarioLogado;
    private final MontagemPdfGuia montagemPdfGuia;

    public GuiaPdfAssinadoService(GuiaRepository guiaRepository,
                                  AssinaturaGuiaRepository assinaturaGuiaRepository,
                                  UsuarioLogado usuarioLogado,
                                  MontagemPdfGuia montagemPdfGuia) {
        this.guiaRepository = guiaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.usuarioLogado = usuarioLogado;
        this.montagemPdfGuia = montagemPdfGuia;
    }

    @Transactional(readOnly = true)
    public PdfGuiaAssinada pdfDaGuia(Long guiaId, String modo) {
        Usuario usuario = usuarioLogado.exigirPerfil(
                PerfilUsuario.MEDICO, PerfilUsuario.CHEFE_FUSEX, PerfilUsuario.BENEFICIARIO);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + guiaId));

        if (usuario.getPerfil() == PerfilUsuario.BENEFICIARIO) {
            return pdfDoBeneficiario(guia, usuario);
        }

        ModoDownload modoEscolhido = ModoDownload.from(modo);
        return new PdfGuiaAssinada(
                new ByteArrayResource(montagemPdfGuia.montar(guia)),
                nomeArquivo(guia),
                modoEscolhido == ModoDownload.BAIXAR);
    }

    private PdfGuiaAssinada pdfDoBeneficiario(Guia guia, Usuario beneficiario) {
        if (!guia.getPreGuia().getBeneficiario().getUsuarioId().equals(beneficiario.getIdUsuario())) {
            throw new AcessoNegadoException("Guia de outro beneficiario: acesso negado");
        }
        if (guia.getPreGuia().getStatus() != PreGuiaStatus.CONCLUIDA) {
            throw new AcessoNegadoException("Pre-guia ainda nao esta concluida: PDF da guia indisponivel");
        }
        long assinaturas = assinaturaGuiaRepository.countByGuiaPreGuiaIdPreGuia(guia.getPreGuia().getIdPreGuia());
        if (assinaturas < 2) {
            throw new AcessoNegadoException("Guia sem as 2 assinaturas necessarias: PDF indisponivel");
        }

        return new PdfGuiaAssinada(
                new ByteArrayResource(montagemPdfGuia.montar(guia)),
                nomeArquivo(guia),
                false);
    }

    private static String nomeArquivo(Guia guia) {
        return "guia-" + guia.getNumero() + ".pdf";
    }
}
