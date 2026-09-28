package fatec.fusex.nexus.ocs.dto;

import java.time.LocalDate;

import fatec.fusex.nexus.ocs.Ocs;

/**
 * DTO de saida de OCS (ARQUITETURA.md: nunca devolver a entidade direto, para nao
 * estourar lazy loading de `procedimentos` fora de transacao).
 */
public record OcsResponse(
        Long id,
        String contratoNum,
        String nome,
        LocalDate inicioVigencia,
        String tipo,
        Integer diasParaVencimento,
        LocalDate terminoVigencia) {

    public static OcsResponse de(Ocs ocs) {
        return new OcsResponse(
                ocs.getId(),
                ocs.getContratoNum(),
                ocs.getNome(),
                ocs.getInicioVigencia(),
                ocs.getTipo(),
                ocs.getDiasParaVencimento(),
                ocs.getTerminoVigencia());
    }
}