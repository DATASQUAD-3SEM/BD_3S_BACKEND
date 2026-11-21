package fatec.fusex.nexus.medicoFusex.dto;

import java.time.LocalDate;
import java.time.Period;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
import fatec.fusex.nexus.dependente.Dependente;


/**
 * Resposta da busca de beneficiario por Prec CP feita pelo medico (T2-BE1).
 * Quando o Prec CP e de um dependente (T30-BE7), o campo dependente traz o bloco do dependente; nos demais, e nulo.
 * O toString omite CPF e telefone para que esses dados nao aparecam em log.
 */
public record BeneficiarioMedicoResponse(
        Long id,
        String nome,
        String grupo,
        String faixaEtaria,
        int idade,
        String cpf,
        String telefone,
        DependenteMedicoBloco dependente) {

    public static final String GRUPO = "Exército Brasileiro · FUSEX";

    public static BeneficiarioMedicoResponse de(Beneficiario b) {
        return de(b, Beneficiario.hoje());
    }

    public static BeneficiarioMedicoResponse de(Beneficiario b, LocalDate hoje) {
        int idade = b.idadeEm(hoje);

        return new BeneficiarioMedicoResponse(
                b.getUsuarioId(),
                b.getNome(),
                GRUPO,
                FaixaEtaria.de(idade).getRotulo(),
                idade,
                b.getUsuario().getLogin(),
                b.getTelefone(),
                null);
    }

    /** Dependente encontrado pelo Prec CP (T30-BE7). Id e o do titular, como na busca de beneficiario. */
    public static BeneficiarioMedicoResponse deDependente(Dependente d, LocalDate hoje, String responsavel) {
        int idade = Period.between(d.getDataNascimento(), hoje).getYears();
        boolean menor = d.getDataNascimento().plusYears(18).isAfter(hoje);
        Beneficiario titular = d.getTitular();

        return new BeneficiarioMedicoResponse(
                titular.getUsuarioId(),
                d.getNome(),
                GRUPO,
                FaixaEtaria.de(idade).getRotulo(),
                idade,
                d.getCpf(),
                d.getTelefone(),
                new DependenteMedicoBloco(
                        d.getIdDependente(),
                        titular.getNome(),
                        titular.getPrecCp(),
                        d.getVinculo(),
                        menor,
                        responsavel));
    }

    @Override
    public String toString() {
        return "BeneficiarioMedicoResponse[id=" + id + "]";
    }
}
