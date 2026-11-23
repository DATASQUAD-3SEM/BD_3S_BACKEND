package fatec.fusex.nexus.medicoFusex.dto;

/**
 * Bloco do dependente na busca do medico (T30-BE7). responsavel: nome de quem age pelo menor (o titular no
 * MENOR_TITULAR, o responsavel legal externo no MENOR_RESPONSAVEL); nulo no maior (regra 13: um so responsavel).
 */
public record DependenteMedicoBloco(
        Long dependenteId,
        String titularNome,
        String titularPrecCp,
        String vinculo,
        boolean menor,
        String responsavel) {
}
