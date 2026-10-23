package fatec.fusex.nexus.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.usuario.dto.AlterarSenhaRequest;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;
    private final PasswordEncoder passwordEncoder;
    private final TentativasSenhaService tentativasSenhaService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            SimulacaoLogin simulacaoLogin,
            PasswordEncoder passwordEncoder,
            TentativasSenhaService tentativasSenhaService) {

        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
        this.passwordEncoder = passwordEncoder;
        this.tentativasSenhaService = tentativasSenhaService;
    }

    @Transactional
    public void alterarSenha(AlterarSenhaRequest request) {
        Usuario usuario = usuarioAtual();

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException(
                    "Usuario inativo nao pode alterar a senha");
        }

        PerfilUsuario perfil = usuario.getPerfil();

        if (perfil != PerfilUsuario.BENEFICIARIO
                && perfil != PerfilUsuario.MEDICO) {
            throw new AcessoNegadoException(
                    "Alteracao de senha nao permitida para este perfil");
        }

        String login = usuario.getLogin();

        tentativasSenhaService.verificarBloqueio(login);

        validarCampos(request);

        String senhaHashAtual = usuario.getSenhaHash();

        if (senhaHashAtual == null
                || !passwordEncoder.matches(
                request.senhaAtual(), senhaHashAtual)) {

            tentativasSenhaService.registrarFalha(login);

            throw new DadosInvalidosException(
                    "senhaAtual",
                    "Senha atual incorreta");
        }

        tentativasSenhaService.registrarSucesso(login);

        if (passwordEncoder.matches(
                request.novaSenha(), senhaHashAtual)) {

            throw new DadosInvalidosException(
                    "novaSenha",
                    "A nova senha deve ser diferente da senha atual");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));

        usuarioRepository.save(usuario);
    }

    private Usuario usuarioAtual() {
        String login = simulacaoLogin.loginAtual();

        return usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));
    }

    private void validarCampos(AlterarSenhaRequest request) {
        if (request == null
                || request.senhaAtual() == null
                || request.senhaAtual().isBlank()) {

            throw new DadosInvalidosException(
                    "senhaAtual",
                    "Informe a senha atual");
        }

        if (request.novaSenha() == null
                || request.novaSenha().isBlank()) {

            throw new DadosInvalidosException(
                    "novaSenha",
                    "Informe a nova senha");
        }

        if (request.novaSenha().length() < 8) {
            throw new DadosInvalidosException(
                    "novaSenha",
                    "A nova senha deve possuir pelo menos 8 caracteres");
        }

        if (request.confirmacao() == null
                || request.confirmacao().isBlank()) {

            throw new DadosInvalidosException(
                    "confirmacao",
                    "Confirme a nova senha");
        }

        if (!request.novaSenha().equals(request.confirmacao())) {
            throw new DadosInvalidosException(
                    "confirmacao",
                    "A confirmacao deve ser igual a nova senha");
        }
    }
}