package fatec.fusex.nexus.ocs.dto;

import fatec.fusex.nexus.ocs.Ocs;

/** DTO de saida de OCS. */
public record OcsResponse(
        Long id,
        String contratoNum,
        String nome,
        String tipo,
	Boolean ativa) {

    public static OcsResponse de(Ocs ocs) {
        return new OcsResponse(
                ocs.getIdOcs(),
                ocs.getContratoNum(),
                ocs.getNome(),
                ocs.getTipo().name(),
		ocs.getAtiva());
    }
}