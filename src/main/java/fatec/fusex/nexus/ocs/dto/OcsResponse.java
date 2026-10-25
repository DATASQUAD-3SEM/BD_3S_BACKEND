package fatec.fusex.nexus.ocs.dto;

import fatec.fusex.nexus.ocs.Ocs;


/**
 * Dados de uma OCS exibidos nas buscas: id, nome, contrato e tipo.
 */
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