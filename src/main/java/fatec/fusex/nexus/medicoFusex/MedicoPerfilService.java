package fatec.fusex.nexus.medicoFusex;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoPerfilResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class MedicoPerfilService {

    private final MedicoFusexRepository medicoFusexRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public MedicoPerfilService(
            MedicoFusexRepository medicoFusexRepository,
            UsuarioRepository usuarioRepository,
            SimulacaoLogin simulacaoLogin) {

        this.medicoFusexRepository = medicoFusexRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @Transactional(readOnly = true)
    public MedicoPerfilResponse buscarPerfilLogado() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.MEDICO) {
            throw new AcessoNegadoException(
                    "Acesso restrito a medicos");
        }

        MedicoFusex medico = medicoFusexRepository
                .findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Perfil medico nao encontrado"));

        return MedicoPerfilResponse.de(medico);
    }
}