package fatec.fusex.nexus.beneficiario.dto;

import java.time.LocalDate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;

/**
 * Resposta do GET /beneficiarios/me (T10-BE1), com idade e faixa etaria ja calculadas.
 * O toString omite CPF e telefone para que esses dados nao aparecam em log.
 */
public record BeneficiarioResponse(
        String nome,
        String grupo,
        String cpf,
        String precCp,
        LocalDate dataNascimento,
        int idade,
        String faixaEtaria,
        String telefone,
        boolean assinaturaCadastrada) {

    public static BeneficiarioResponse de(Beneficiario b) {
        return de(b, Beneficiario.hoje());
    }

    public static BeneficiarioResponse de(Beneficiario b, LocalDate hoje) {
        int idade = b.idadeEm(hoje);

        return new BeneficiarioResponse(
                b.getNome(),
                BeneficiarioMedicoResponse.GRUPO,
                b.getUsuario().getLogin(),
                b.getPrecCp(),
                b.getDataNascimento(),
                idade,
                FaixaEtaria.de(idade).getRotulo(),
                b.getTelefone(),
                b.getUsuario().getAssinatura() != null);
    }

    // CPF e telefone nao podem ir para log.
    @Override
    public String toString() {
        return "BeneficiarioResponse[idade=" + idade + ", assinaturaCadastrada=" + assinaturaCadastrada + "]";
    }
}