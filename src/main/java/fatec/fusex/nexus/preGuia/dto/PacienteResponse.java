package fatec.fusex.nexus.preGuia.dto;

/** Paciente possivel para uma nova pre-guia. dependenteId nulo = o proprio titular (T30-BE2). */
public record PacienteResponse(Long dependenteId, String nome, String relacao) {
}
