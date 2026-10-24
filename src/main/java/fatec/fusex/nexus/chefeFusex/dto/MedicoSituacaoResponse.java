package fatec.fusex.nexus.chefeFusex.dto;

/** Resposta do PATCH /chefe/medicos/{id}/situacao (T8-BE4): a situacao que ficou gravada. */
public record MedicoSituacaoResponse(Long id, boolean ativo) {
}
