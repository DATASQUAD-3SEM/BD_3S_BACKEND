package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import java.time.LocalDate;

/**
 * Edicao de dependente pelo chefe (T31-BE3). O responsavel nao e editavel nesta versao, e CPF e data de nascimento
 * tambem nao mudam (o CPF e o login da conta). tipoVigencia: VITALICIA ou DETERMINADA.
 */
public record EditarDependenteRequest(
        String nome,
        String telefone,
        String precCp,
        String vinculo,
        String tipoVigencia,
        LocalDate vigenciaFim) {
}
