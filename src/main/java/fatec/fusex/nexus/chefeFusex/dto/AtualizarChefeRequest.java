package fatec.fusex.nexus.chefeFusex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PUT /chefe/me. novaSenha vazia mantem a senha atual.
 */
public record AtualizarChefeRequest(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "A patente é obrigatória.")
        @Size(max = 50, message = "A patente deve ter no máximo 50 caracteres.")
        String patente,

        @NotBlank(message = "O login é obrigatório.")
        @Size(max = 50, message = "O login deve ter no máximo 50 caracteres.")
        String login,

        String novaSenha

) {
}