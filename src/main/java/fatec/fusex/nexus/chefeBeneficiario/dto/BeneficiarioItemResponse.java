package fatec.fusex.nexus.chefeBeneficiario.dto;

import fatec.fusex.nexus.beneficiario.Beneficiario;

/**
 * Linha da tabela de beneficiarios do Chefe (T7-BE1). dependentesAtivos conta so os vigentes (T31-BE1).
 */
public record BeneficiarioItemResponse(
        Long id, String nome, String cpf, String precCp, boolean ativo, int dependentesAtivos) {

    public static BeneficiarioItemResponse de(Beneficiario b, int dependentesAtivos) {
        return new BeneficiarioItemResponse(
                b.getUsuarioId(), b.getNome(), b.getUsuario().getLogin(),
                b.getPrecCp(), b.getUsuario().getAtivo(), dependentesAtivos);
    }
}
