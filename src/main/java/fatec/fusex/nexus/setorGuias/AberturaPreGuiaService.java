package fatec.fusex.nexus.setorGuias;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.setorGuias.dto.AberturaPreGuiaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T4-BE3: POST /setor-guias/pre-guias/{id}/abertura.
 * Pendente vira Em analise e o historico registra o ator SETOR_GUIAS (conta de setor, sem pessoa).
 * Em qualquer outro status nao altera nada (idempotente).
 */
@Service
public class AberturaPreGuiaService {

    private final AberturaPreGuiaRepository preGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final UsuarioLogado usuarioLogado;

    public AberturaPreGuiaService(AberturaPreGuiaRepository preGuiaRepository,
                                  PreGuiaHistoricoRepository historicoRepository,
                                  UsuarioLogado usuarioLogado) {
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Ordem: perfil (403) -> existe (404)
    @Transactional
    public AberturaPreGuiaResponse abrir(Long id) {
        Usuario setor = usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

        // Trava a linha ate o fim da transacao (duas aberturas simultaneas geram uma unica linha de historico)
        PreGuia preGuia = preGuiaRepository.buscarParaAbrir(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + id));

        if (preGuia.getStatus() == PreGuiaStatus.PENDENTE) {
            preGuia.setStatus(PreGuiaStatus.EM_ANALISE);
            historicoRepository.save(new PreGuiaHistorico(
                    null, preGuia, PreGuiaStatus.EM_ANALISE, null, null, setor, LocalDateTime.now()));
        }

        return new AberturaPreGuiaResponse(preGuia.getStatus().name());
    }
}
