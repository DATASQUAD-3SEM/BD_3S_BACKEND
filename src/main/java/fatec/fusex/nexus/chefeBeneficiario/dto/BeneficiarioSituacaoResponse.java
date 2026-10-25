package fatec.fusex.nexus.chefeBeneficiario.dto;

/** Resposta do PATCH /chefe/beneficiarios/{id}/situacao (T7-BE4): a situacao que ficou gravada. */
public record BeneficiarioSituacaoResponse(Long id, boolean ativo) {
}
