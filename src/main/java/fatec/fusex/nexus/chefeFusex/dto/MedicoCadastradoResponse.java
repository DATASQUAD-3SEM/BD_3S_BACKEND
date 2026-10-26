package fatec.fusex.nexus.chefeFusex.dto;

/** Resposta do POST /chefe/medicos (T8-BE2): so o id, sem expor a senha. */
public record MedicoCadastradoResponse(Long id) {
}
