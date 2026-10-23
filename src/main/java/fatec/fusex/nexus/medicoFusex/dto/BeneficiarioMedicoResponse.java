package fatec.fusex.nexus.medicoFusex.dto;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Resposta de GET /medicos/beneficiarios?precCp=...
 * Idade e faixa etaria saem da data de nascimento (nao sao gravadas no banco).
 */
public record BeneficiarioMedicoResponse(
        Long id,
        String nome,
        String grupo,
        String faixaEtaria,
        int idade,
        String cpf,
        String telefone) {


    public static final String GRUPO = "Ex\u00e9rcito Brasileiro \u00b7 FUSEX";

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public static BeneficiarioMedicoResponse de(Beneficiario b) {
        return de(b, LocalDate.now(FUSO));
    }

    // Versao com a data de hoje explicita (usada nos testes).
    public static BeneficiarioMedicoResponse de(Beneficiario b, LocalDate hoje) {
        Usuario u = b.getUsuario();
        int idade = Period.between(b.getDataNascimento(), hoje).getYears();

        return new BeneficiarioMedicoResponse(
                b.getUsuarioId(),
                b.getNome(),
                GRUPO,
                FaixaEtaria.de(idade).getRotulo(),
                idade,
                u.getLogin(),
                b.getTelefone());
    }

    // CPF e telefone nao podem ir para log: o toString padrao do record imprimiria tudo.
    @Override
    public String toString() {
        return "BeneficiarioMedicoResponse[id=" + id + "]";
    }
}
