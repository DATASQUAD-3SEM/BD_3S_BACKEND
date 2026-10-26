package fatec.fusex.nexus.chefeBeneficiario.dto;

/**
 * Corpo do PUT /chefe/beneficiarios/{id} (T7-BE3).
 * novaSenha e opcional: nula ou vazia mantem a senha atual.
 * As validacoes ficam no ChefeBeneficiarioEdicaoService, porque a task pede 422 apontando o campo.
 */
public record EditarBeneficiarioRequest(
        String nome,
        String cpf,
        String precCp,
        String dataNascimento,
        String telefone,
        Boolean ativo,
        String novaSenha) {

    // Nunca registrar senha, CPF nem telefone em log.
    @Override
    public String toString() {
        return "EditarBeneficiarioRequest[nome=" + nome + ", ativo=" + ativo
                + ", cpf=***, telefone=***, novaSenha=***]";
    }
}
