package fatec.fusex.nexus.common.auth;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Centraliza a identificacao do usuario logado e a checagem de perfil feita no inicio de cada endpoint.
 *
 * O login vem do {@link SimulacaoLogin} (header X-Usuario-Login). Os servicos devem chamar um dos
 * metodos abaixo antes de qualquer regra de negocio, para que o 401/403 aconteca antes das validacoes.
 */
@Component
public class UsuarioLogado {

    private final SimulacaoLogin simulacaoLogin;
    private final UsuarioRepository usuarioRepository;

    public UsuarioLogado(SimulacaoLogin simulacaoLogin, UsuarioRepository usuarioRepository) {
        this.simulacaoLogin = simulacaoLogin;
        this.usuarioRepository = usuarioRepository;
    }

    /** Retorna o usuario do login atual, ou 401 se o login nao corresponder a nenhuma conta. */
    public Usuario atual() {
        String login = simulacaoLogin.loginAtual();
        return usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));
    }

    /** Exige que o usuario logado tenha um dos perfis informados (403 caso contrario). */
    public Usuario exigirPerfil(PerfilUsuario... perfis) {
        Usuario usuario = atual();
        if (Arrays.stream(perfis).noneMatch(perfil -> perfil == usuario.getPerfil())) {
            String nomes = Arrays.stream(perfis).map(Enum::name).collect(Collectors.joining(" ou "));
            throw new AcessoNegadoException("Acesso negado: este endpoint e exclusivo do perfil " + nomes);
        }
        return usuario;
    }

    /** Igual a {@link #exigirPerfil}, mas tambem bloqueia contas desativadas (403). */
    public Usuario exigirPerfilAtivo(PerfilUsuario... perfis) {
        Usuario usuario = exigirPerfil(perfis);
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Acesso negado: usuario inativo");
        }
        return usuario;
    }
}
