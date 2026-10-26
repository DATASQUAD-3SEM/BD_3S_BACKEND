package fatec.fusex.nexus.beneficiario.dto;

/**
 * Resposta do PATCH /beneficiarios/me/telefone: devolve o telefone como ficou gravado.
 */
public record TelefoneAtualizadoResponse(String telefone) {
}