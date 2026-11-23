package fatec.fusex.nexus.preGuia.dto;

import java.time.LocalDate;
import java.time.Period;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
import fatec.fusex.nexus.dependente.Dependente;

/**
 * Paciente possivel para uma nova pre-guia (T30-BE2). dependenteId nulo = o proprio titular. Traz os dados do bloco
 * "Dados do paciente" da Nova pre-guia (T30-FE1): o titular vem do cadastro de beneficiario; o dependente, do cadastro
 * de dependente. O toString omite CPF e telefone para que esses dados nao aparecam em log.
 */
public record PacienteResponse(
        Long dependenteId,
        String nome,
        String relacao,
        LocalDate dataNascimento,
        int idade,
        String faixaEtaria,
        String precCp,
        String cpf,
        String telefone) {

    public static PacienteResponse doTitular(Beneficiario titular, String relacao, LocalDate hoje) {
        int idade = titular.idadeEm(hoje);
        return new PacienteResponse(null, titular.getNome(), relacao, titular.getDataNascimento(), idade,
                FaixaEtaria.de(idade).getRotulo(), titular.getPrecCp(), titular.getUsuario().getLogin(),
                titular.getTelefone());
    }

    public static PacienteResponse doDependente(Dependente dependente, String relacao, LocalDate hoje) {
        int idade = Period.between(dependente.getDataNascimento(), hoje).getYears();
        return new PacienteResponse(dependente.getIdDependente(), dependente.getNome(), relacao,
                dependente.getDataNascimento(), idade, FaixaEtaria.de(idade).getRotulo(), dependente.getPrecCp(),
                dependente.getCpf(), dependente.getTelefone());
    }

    // CPF e telefone nao podem ir para log.
    @Override
    public String toString() {
        return "PacienteResponse[dependenteId=" + dependenteId + ", relacao=" + relacao + ", idade=" + idade + "]";
    }
}
