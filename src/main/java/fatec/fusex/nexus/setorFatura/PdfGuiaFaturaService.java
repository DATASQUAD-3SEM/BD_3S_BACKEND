package fatec.fusex.nexus.setorFatura;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.MontagemPdfGuia;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@Service
public class PdfGuiaFaturaService {

    private static final Set<SituacaoFatura> CHEGOU_AO_SETOR = EnumSet.of(
            SituacaoFatura.EM_LISURA,
            SituacaoFatura.AGUARDANDO_OCS,
            SituacaoFatura.EM_AUDITORIA,
            SituacaoFatura.AUDITADA);

    private final UsuarioLogado usuarioLogado;
    private final GuiaRepository guiaRepository;
    private final MontagemPdfGuia montagemPdfGuia;

    public PdfGuiaFaturaService(UsuarioLogado usuarioLogado,
                                GuiaRepository guiaRepository,
                                MontagemPdfGuia montagemPdfGuia) {
        this.usuarioLogado = usuarioLogado;
        this.guiaRepository = guiaRepository;
        this.montagemPdfGuia = montagemPdfGuia;
    }

    @Transactional(readOnly = true)
    public PdfGuiaFatura pdfDaGuia(Long guiaId, PerfilUsuario perfil) {
        usuarioLogado.exigirPerfil(perfil);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> naoEncontrada(guiaId));

        Espelho espelho = guia.getEspelho();
        if (espelho == null || !CHEGOU_AO_SETOR.contains(espelho.getSituacaoFatura())) {
            throw naoEncontrada(guiaId);
        }

        return new PdfGuiaFatura(
                new ByteArrayResource(montagemPdfGuia.montar(guia)),
                "guia-" + guia.getNumero() + ".pdf");
    }

    private static RecursoNaoEncontradoException naoEncontrada(Long guiaId) {
        return new RecursoNaoEncontradoException("Guia nao encontrada nas faturas do setor: id " + guiaId);
    }
}