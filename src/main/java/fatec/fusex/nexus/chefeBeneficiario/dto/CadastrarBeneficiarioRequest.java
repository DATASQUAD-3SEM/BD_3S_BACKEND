package fatec.fusex.nexus.chefeBeneficiario.dto;

/**
 * Corpo do POST /chefe/beneficiarios (T7-BE2).
 * dataNascimento vem como texto (aaaa-mm-dd) para a data invalida virar 422 no campo, e nao 400 de JSON.
 * As validacoes ficam no ChefeBeneficiarioCadastroService, porque a task pede 422 apontando o campo.
 */
public record CadastrarBeneficiarioRequest(
        String nome,
        String cpf,
        String precCp,
        String dataNascimento,
        String telefone,
        String senha) {

    // Nunca registrar senha, CPF nem telefone em log.
    @Override
    public String toString() {
        return "CadastrarBeneficiarioRequest[nome=" + nome + ", cpf=***, telefone=***, senha=***]";
    }
}
