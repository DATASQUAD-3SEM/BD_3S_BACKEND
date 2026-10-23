package fatec.fusex.nexus.beneficiario.dto;

import fatec.fusex.nexus.beneficiario.Beneficiario;

import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.usuario.Usuario;

import java.time.LocalDate;

public record BeneficiarioResponse(
        String nome,
        String cpf,
        String precCp,
        String telefone,
        LocalDate dataNascimento,
        String grupo,
        Boolean assinaturaCadastrada) {

    public static BeneficiarioResponse de(Beneficiario b) {
        Usuario u = b.getUsuario();

        return new BeneficiarioResponse(
                b.getNome(),
                u.getLogin(),
                b.getPrecCp(),
                b.getTelefone(),
                b.getDataNascimento(),
                BeneficiarioMedicoResponse.GRUPO,
                u.getAssinaturaUrl() != null
        );
    }
}
