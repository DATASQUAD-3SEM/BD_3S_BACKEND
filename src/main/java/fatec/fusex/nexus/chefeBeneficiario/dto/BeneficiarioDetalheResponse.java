package fatec.fusex.nexus.chefeBeneficiario.dto;

import java.time.LocalDate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;

/**
 * Dados do beneficiario no modo de edicao do chefe (T7-BE3). Nunca expoe a senha.
 * idade e faixaEtaria nao sao gravadas: saem da data de nascimento a cada consulta.
 */
public record BeneficiarioDetalheResponse(
        Long id,
        String nome,
        String cpf,
        String precCp,
        LocalDate dataNascimento,
        String telefone,
        String faixaEtaria,
        int idade,
        boolean ativo) {

    public static BeneficiarioDetalheResponse de(Beneficiario b) {
        int idade = b.idadeEm(Beneficiario.hoje());
        return new BeneficiarioDetalheResponse(
                b.getUsuarioId(),
                b.getNome(),
                b.getUsuario().getLogin(),
                b.getPrecCp(),
                b.getDataNascimento(),
                b.getTelefone(),
                FaixaEtaria.de(idade).getRotulo(),
                idade,
                Boolean.TRUE.equals(b.getUsuario().getAtivo()));
    }

    // Nao registrar CPF nem telefone em log.
    @Override
    public String toString() {
        return "BeneficiarioDetalheResponse[id=" + id + "]";
    }
}
