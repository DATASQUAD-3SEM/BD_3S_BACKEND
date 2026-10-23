package fatec.fusex.nexus.usuario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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