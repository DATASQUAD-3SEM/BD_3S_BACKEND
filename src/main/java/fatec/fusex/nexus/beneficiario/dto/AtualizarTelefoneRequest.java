package fatec.fusex.nexus.beneficiario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo do PATCH /beneficiarios/me/telefone. Qualquer outro campo enviado e ignorado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AtualizarTelefoneRequest(String telefone) {
}