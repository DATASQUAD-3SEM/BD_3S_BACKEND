package fatec.fusex.nexus.setorGuias.dto;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;


/**
 * Linha da fila de pre-guias do Setor de Guias (T3-BE1).
 */
public record FilaPreGuiaItemResponse(
        Long id,
        String numero,
        String beneficiarioNome,
        String ocsNome,
        String origem,
        LocalDateTime dataCriacao,
        String status) {

    public static FilaPreGuiaItemResponse de(PreGuia p) {
        String origem = p.getMedicoFusex() != null ? "MEDICO" : "BENEFICIARIO";
        return new FilaPreGuiaItemResponse(
                p.getIdPreGuia(),
                String.format("%04d", p.getIdPreGuia()),
                p.getBeneficiario().getNome(),
                p.getOcs().getNome(),
                origem,
                p.getDataCriacao(),
                p.getStatus().name());
    }
}