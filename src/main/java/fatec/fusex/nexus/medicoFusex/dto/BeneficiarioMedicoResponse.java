package fatec.fusex.nexus.medicoFusex.dto;

import java.time.LocalDate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;


public record BeneficiarioMedicoResponse(
        Long id,
        String nome,
        String grupo,
        String faixaEtaria,
        int idade,
        String cpf,
        String telefone) {

    public static final String GRUPO = "Ex\u00e9rcito Brasileiro \u00b7 FUSEX";

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
                b.getTelefone());
    }

    @Override
    public String toString() {
        return "BeneficiarioMedicoResponse[id=" + id + "]";
    }
}