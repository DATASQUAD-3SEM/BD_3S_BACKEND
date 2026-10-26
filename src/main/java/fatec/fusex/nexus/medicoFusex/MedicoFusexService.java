package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;

/**
 * Regras comuns do perfil medico: confirma que o usuario logado e um medico ativo (403)
 * e que possui cadastro de medico (404).
 */
@Service
public class MedicoFusexService {

    private final MedicoFusexRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;

    public MedicoFusexService(MedicoFusexRepository medicoRepository,
                              UsuarioRepository usuarioRepository) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
    }


    public Usuario validarPerfilMedico(String login) {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new AcessoNegadoException("Acesso restrito a medicos"));

        if (!Boolean.TRUE.equals(usuario.getAtivo()) || usuario.getPerfil() != PerfilUsuario.MEDICO) {
            throw new AcessoNegadoException("Acesso restrito a medicos");
        }
        return usuario;
    }

    // Valida se o usuário logado é médico e está ativo (exige tambem o cadastro em MedicoFusex).
    public MedicoFusex validarAcessoMedico(String login) {
        MedicoFusex medico = medicoRepository.findByUsuarioLogin(login)
                .orElseThrow(() -> new AcessoNegadoException("Acesso restrito a medicos"));

        if (!medico.getUsuario().getAtivo() || !PerfilUsuario.MEDICO.equals(medico.getUsuario().getPerfil())) {
            throw new AcessoNegadoException("Acesso restrito a medicos");
        }
        return medico;
    }


    public MedicoMeResponse buscarLogado(String login) {
        validarPerfilMedico(login);
        MedicoFusex medico = medicoRepository.findByUsuarioLogin(login)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cadastro de medico nao encontrado"));
        return MedicoMeResponse.de(medico);
    }
}
