package fatec.fusex.nexus.setorGuias;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.setorGuias.dto.AberturaPreGuiaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T4-BE3: POST /setor-guias/pre-guias/{id}/abertura.
 * Pendente vira Em analise e o historico registra o ator SETOR_GUIAS (conta de setor, sem pessoa).
 * Em qualquer outro status nao altera nada (idempotente).
 */
@Service
public class AberturaPreGuiaService {

    private final AberturaPreGuiaRepository preGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public AberturaPreGuiaService(AberturaPreGuiaRepository preGuiaRepository,
                                  PreGuiaHistoricoRepository historicoRepository,
                                  UsuarioRepository usuarioRepository,
                                  SimulacaoLogin simulacaoLogin) {
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    // Ordem: perfil (403) -> existe (404)
    @Transactional
    public AberturaPreGuiaResponse abrir(Long id) {
        Usuario setor = validarSetorGuias();

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

    private Usuario validarSetorGuias() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.SETOR_GUIAS) {
            throw new AcessoNegadoException("Acesso negado: este endpoint e exclusivo do SETOR_GUIAS");
        }
        return usuario;
    }
}
