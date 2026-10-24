package fatec.fusex.nexus.ocs.dto;

import fatec.fusex.nexus.ocs.Ocs;


public record OcsResponse(
        Long id,
        String contrato,
        String nome,
        String tipo) {

    public static OcsResponse de(Ocs ocs) {
        return new OcsResponse(
                ocs.getIdOcs(),
                ocs.getContratoNum(),
                ocs.getNome(),
                ocs.getTipo().name());
    }
}