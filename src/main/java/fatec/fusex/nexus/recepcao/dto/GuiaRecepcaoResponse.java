package fatec.fusex.nexus.recepcao.dto;

import java.time.LocalDate;

import fatec.fusex.nexus.guia.Guia;

/**
 * Linha da busca de Guias da recepcao (T6-BE1).
 */
public record GuiaRecepcaoResponse(
        Long guiaId,
        String numero,
        String beneficiarioNome,
        String precCp,
        String ocsNome,
        LocalDate validadeAte) {

    public static GuiaRecepcaoResponse de(Guia guia) {
        return new GuiaRecepcaoResponse(
                guia.getIdGuia(),
                guia.getNumero(),
                guia.getPreGuia().getBeneficiario().getNome(),
                guia.getPreGuia().getBeneficiario().getPrecCp(),
                guia.getPreGuia().getOcs().getNome(),
                guia.validadeAte());
    }
}