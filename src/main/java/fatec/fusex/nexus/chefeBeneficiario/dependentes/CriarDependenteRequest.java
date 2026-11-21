package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import java.time.LocalDate;

/**
 * Cadastro de dependente pelo chefe (T31-BE2). tipoVigencia: VITALICIA ou DETERMINADA.
 * responsavel: obrigatorio para menor (TITULAR ou OUTRA_PESSOA); proibido para maior.
 * senha: definida pelo chefe quando cria conta nova (maior, ou responsavel externo novo).
 */
public record CriarDependenteRequest(
        String nome,
        LocalDate dataNascimento,
        String cpf,
        String telefone,
        String precCp,
        String vinculo,
        String tipoVigencia,
        LocalDate vigenciaFim,
        ResponsavelRequest responsavel,
        String senha) {

    public record ResponsavelRequest(String tipo, String cpf, String nome, String telefone) {
    }
}
