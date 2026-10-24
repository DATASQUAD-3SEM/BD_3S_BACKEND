package fatec.fusex.nexus.medicoFusex.dto;

import fatec.fusex.nexus.medicoFusex.MedicoFusex;

public record MedicoMeResponse(
        Long id,
        String nome,
        String patente,
        String crm) {

    public static MedicoMeResponse de(MedicoFusex m) {
        return new MedicoMeResponse(
                m.getUsuarioId(),
                m.getNome(),
                m.getPatente(),
                m.getCrm());
    }

    // O ID não é dado sensível, mas mantemos o toString limpo por padrão do projeto.
    @Override
    public String toString() {
        return "MedicoMeResponse[id=" + id + "]";
    }
}