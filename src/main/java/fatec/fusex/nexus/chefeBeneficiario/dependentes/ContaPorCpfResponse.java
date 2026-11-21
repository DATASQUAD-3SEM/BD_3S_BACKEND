package fatec.fusex.nexus.chefeBeneficiario.dependentes;

/**
 * Conta ja existente para um CPF, para o formulario de dependente (T31-BE2 / T31-FE3) avisar antes de salvar.
 * situacao: NOVA (sem conta), RESPONSAVEL_EXTERNO, DEPENDENTE_MAIOR_VITALICIO ou OUTRA (titular, dependente com
 * vigencia determinada ou outro perfil: nao pode ser reaproveitada como responsavel legal). nome so quando reaproveitavel.
 */
public record ContaPorCpfResponse(String situacao, String nome) {
}
