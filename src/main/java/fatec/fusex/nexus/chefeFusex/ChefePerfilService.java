package fatec.fusex.nexus.chefeFusex;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class ChefePerfilService {

    private final ChefeFusexRepository chefeFusexRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public ChefePerfilService(
            ChefeFusexRepository chefeFusexRepository,
            UsuarioRepository usuarioRepository,
            SimulacaoLogin simulacaoLogin) {

        this.chefeFusexRepository = chefeFusexRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @Transactional(readOnly = true)
    public ChefePerfilResponse buscarPerfilLogado() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.CHEFE_FUSEX) {
            throw new AcessoNegadoException(
                    "Acesso restrito ao chefe do FUSEX");
        }

        ChefeFusex chefe = chefeFusexRepository
                .findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Perfil do chefe do FUSEX nao encontrado"));

        return ChefePerfilResponse.de(chefe);
    }
}