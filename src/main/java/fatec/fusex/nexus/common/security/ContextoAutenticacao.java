package fatec.fusex.nexus.common.security;

import java.security.Principal;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import jakarta.servlet.http.HttpServletRequest;

public final class ContextoAutenticacao {

    private ContextoAutenticacao() {
    }

    /** @throws AcessoNegadoException (403) se o usuario nao tem o perfil exigido. */
    public static void exigirPerfil(HttpServletRequest requisicao, PerfilUsuario perfil) {
        if (!requisicao.isUserInRole(perfil.name())) {
            throw new AcessoNegadoException("Acesso restrito ao perfil " + perfil.name());
        }
    }


    public static String loginDe(HttpServletRequest requisicao) {
        Principal principal = requisicao.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new NaoAutenticadoException("Autenticacao necessaria");
        }
        return principal.getName();
    }
}
