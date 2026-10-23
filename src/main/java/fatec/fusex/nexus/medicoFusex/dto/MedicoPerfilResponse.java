package fatec.fusex.nexus.medicoFusex.dto;

import fatec.fusex.nexus.medicoFusex.MedicoFusex;

public record MedicoPerfilResponse(
        String nome,
        String patente,
        String cpf,
        String precCp,
        String crm,
        boolean assinaturaCadastrada) {

    public static MedicoPerfilResponse de(MedicoFusex medico) {
        String assinaturaUrl = medico.getUsuario().getAssinaturaUrl();

        return new MedicoPerfilResponse(
                medico.getNome(),
                medico.getPatente(),
                medico.getUsuario().getLogin(),
                medico.getPrecCp(),
                medico.getCrm(),
                assinaturaUrl != null && !assinaturaUrl.isBlank()
        );
    }

    @Override
    public String toString() {
        return "MedicoPerfilResponse[perfil=MEDICO]";
    }
}