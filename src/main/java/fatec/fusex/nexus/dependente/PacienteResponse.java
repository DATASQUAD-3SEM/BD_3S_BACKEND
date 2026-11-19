package fatec.fusex.nexus.dependente;

/**
 * Um paciente que o usuario pode escolher na nova pre-guia (T30-BE2). dependenteId e nulo quando o paciente e o
 * proprio titular.
 */
public record PacienteResponse(Long dependenteId, String nome, String relacao) {
}
