package fatec.fusex.nexus.usuario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo do POST /usuarios/me/senha (T13-BE1). O toString omite as senhas para que nunca aparecam em log.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlterarSenhaRequest(
        String senhaAtual,
        String novaSenha,
        String confirmacao
) {

    @Override
    public String toString() {
        return "AlterarSenhaRequest[credenciais ocultas]";
    }
}