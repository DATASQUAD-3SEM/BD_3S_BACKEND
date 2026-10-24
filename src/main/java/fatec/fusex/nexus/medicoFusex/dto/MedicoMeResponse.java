package fatec.fusex.nexus.medicoFusex.dto;

import fatec.fusex.nexus.medicoFusex.MedicoFusex;


public record MedicoMeResponse(
        String nome,
        String patente,
        String crm) {

    public static MedicoMeResponse de(MedicoFusex m) {
        return new MedicoMeResponse(
                m.getNome(),
                m.getPatente(),
                m.getCrm());
    }

    // Mantemos o toString sem dados do medico, por padrao do projeto.
    @Override
    public String toString() {
        return "MedicoMeResponse[crm=***]";
    }
}
