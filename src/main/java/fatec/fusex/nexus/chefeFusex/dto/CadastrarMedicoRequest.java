package fatec.fusex.nexus.chefeFusex.dto;

/**
 * Corpo do POST /chefe/medicos (T8-BE2).
 * As validacoes ficam no ChefeMedicoCadastroService, porque a task pede 422 apontando o campo.
 */
public record CadastrarMedicoRequest(
        String nome,
        String patente,
        String cpf,
        String precCp,
        String crm,
        String senha) {

    // Nunca registrar senha nem CPF em log.
    @Override
    public String toString() {
        return "CadastrarMedicoRequest[nome=" + nome + ", senha=***, cpf=***]";
    }
}
