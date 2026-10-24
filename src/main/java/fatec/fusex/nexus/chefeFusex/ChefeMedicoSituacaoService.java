package fatec.fusex.nexus.chefeFusex;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.chefeFusex.dto.AlterarSituacaoMedicoRequest;
import fatec.fusex.nexus.chefeFusex.dto.MedicoSituacaoResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T8-BE4: desativar e reativar medico.
 * Nunca faz DELETE: so altera Usuario.ativo, para preservar pre-guias, Guias e assinaturas.
 * O bloqueio de login e de assinatura do inativo fica nos endpoints de login e de assinatura.
 */
@Service
public class ChefeMedicoSituacaoService {

    private final MedicoFusexRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public ChefeMedicoSituacaoService(MedicoFusexRepository medicoRepository,
                                      UsuarioRepository usuarioRepository,
                                      SimulacaoLogin simulacaoLogin) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    // Ordem: perfil (403) -> existe (404) -> corpo (422). Repetir a mesma situacao devolve 200 (idempotente).
    @Transactional
    public MedicoSituacaoResponse alterar(Long id, AlterarSituacaoMedicoRequest request) {
        validarChefe();

        MedicoFusex medico = medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medico nao encontrado"));

        if (request == null || request.ativo() == null) {
            throw new DadosInvalidosException("ativo", "Campo obrigatorio");
        }

        Usuario usuario = medico.getUsuario();
        usuario.setAtivo(request.ativo());

        return new MedicoSituacaoResponse(medico.getUsuarioId(), usuario.getAtivo());
    }

    private void validarChefe() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.CHEFE_FUSEX || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Acesso restrito ao chefe do FUSEX");
        }
    }
}
