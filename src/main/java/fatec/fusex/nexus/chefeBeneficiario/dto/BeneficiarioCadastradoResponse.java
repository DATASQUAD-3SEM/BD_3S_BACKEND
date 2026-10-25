package fatec.fusex.nexus.chefeBeneficiario.dto;

/** Resposta do POST /chefe/beneficiarios (T7-BE2): so o id, sem expor a senha. */
public record BeneficiarioCadastradoResponse(Long id) {
}
