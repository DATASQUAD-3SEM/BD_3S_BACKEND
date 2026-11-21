package fatec.fusex.nexus.chefeBeneficiario.dependentes;

/** Responsavel legal de um dependente. papel: TITULAR (menor sob guarda do titular) ou EXTERNO. */
public record ResponsavelDependenteResponse(Long usuarioId, String nome, String papel) {
}
