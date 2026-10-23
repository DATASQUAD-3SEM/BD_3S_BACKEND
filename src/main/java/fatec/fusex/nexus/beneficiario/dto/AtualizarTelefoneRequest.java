package fatec.fusex.nexus.beneficiario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtualizarTelefoneRequest(String telefone) {
}