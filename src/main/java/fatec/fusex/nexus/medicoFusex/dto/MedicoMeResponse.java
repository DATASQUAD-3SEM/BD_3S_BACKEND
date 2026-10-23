package fatec.fusex.nexus.medicoFusex.dto;

import fatec.fusex.nexus.medicoFusex.MedicoFusex;


public record MedicoMeResponse(
        Long id,
        String nome,
        String patente,
        String crm,
        String precCp) {

    public static MedicoMeResponse de(MedicoFusex m) {
        return new MedicoMeResponse(
                m.getUsuarioId(),
                m.getNome(),
                m.getPatente(),
                m.getCrm(),
                m.getPrecCp());
    }

    // Prec CP nao vai para log: o toString padrao do record imprimiria tudo.
    @Override
    public String toString() {
        return "MedicoMeResponse[id=" + id + "]";
    }
}
