
package fatec.fusex.nexus.chefeFusex;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.chefeFusex.dto.AtualizarChefeRequest;
import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;

@RequiredArgsConstructor
@Service
public class ChefePerfilService {

    private final ChefeFusexRepository chefeFusexRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public ChefePerfilResponse atualizarPerfil(
            AtualizarChefeRequest request
    ) {
        String loginAtual = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(loginAtual)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário logado não encontrado."
                ));

        if (usuario.getPerfil() != PerfilUsuario.CHEFE_FUSEX) {
            throw new AcessoNegadoException(
                    "Apenas o Chefe do FuSEx pode atualizar este perfil."
            );
        }

        ChefeFusex chefe = chefeFusexRepository
                .findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Perfil do Chefe FuSEx não encontrado."
                ));

        String novoNome = campoObrigatorio(request.nome(), "Nome");
        String novaPatente = campoObrigatorio(request.patente(), "Patente");
        String novoLogin = campoObrigatorio(request.login(), "Login");

        String novaSenha = request.novaSenha();

        if (novaSenha != null
                && !novaSenha.isBlank()
                && novaSenha.length() < 8) {
            throw new RegraDeNegocioException(
                    "A nova senha deve ter pelo menos 8 caracteres."
            );
        }

        if (novoLogin.chars().anyMatch(Character::isWhitespace)) {
            throw new DadosInvalidosException(
                    "login",
                    "O login não pode conter espaços."
            );
        }

        var usuarioComMesmoLogin = usuarioRepository.findByLogin(novoLogin);

        if (usuarioComMesmoLogin.isPresent()
                && !Objects.equals(
                usuarioComMesmoLogin.get().getIdUsuario(),
                usuario.getIdUsuario()
        )) {
            throw new ConflitoException(
                    "Já existe um usuário com esse login."
            );
        }

        chefe.setNome(novoNome);
        chefe.setPatente(novaPatente);
        usuario.setLogin(novoLogin);

        if (novaSenha != null && !novaSenha.isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        }

        chefeFusexRepository.save(chefe);
        usuarioRepository.save(usuario);

        boolean assinaturaCadastrada =
                usuario.getAssinaturaUrl() != null
                        && !usuario.getAssinaturaUrl().isBlank();

        return new ChefePerfilResponse(
                chefe.getNome(),
                chefe.getPatente(),
                usuario.getLogin(),
                assinaturaCadastrada
        );
    }

    private String campoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException(
                    campo + " é obrigatório."
            );
        }

        return valor.trim();
    }
}
