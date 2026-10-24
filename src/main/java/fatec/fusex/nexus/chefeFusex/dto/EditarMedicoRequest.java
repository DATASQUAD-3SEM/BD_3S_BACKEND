package fatec.fusex.nexus.chefeFusex.dto;

/**
 * Corpo do PUT /chefe/medicos/{id} (T8-BE3).
 * novaSenha e opcional: nula ou vazia mantem a senha atual.
 * As validacoes ficam no ChefeMedicoEdicaoService, porque a task pede 422 apontando o campo.
 */
public record EditarMedicoRequest(
        String nome,
        String patente,
        String cpf,
        String precCp,
        String crm,
        Boolean ativo,
        String novaSenha) {

    // Nunca registrar senha nem CPF em log.
    @Override
    public String toString() {
        return "EditarMedicoRequest[nome=" + nome + ", ativo=" + ativo + ", novaSenha=***, cpf=***]";
    }
}
