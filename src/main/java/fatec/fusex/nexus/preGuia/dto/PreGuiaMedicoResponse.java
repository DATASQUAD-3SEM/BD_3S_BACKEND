package fatec.fusex.nexus.preGuia.dto;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;

/** Resposta do POST /medicos/pre-guias (T2-BE4). */
public record PreGuiaMedicoResponse(
        Long id,
        String numero,
        String status,
        LocalDateTime dataCriacao) {

    public static PreGuiaMedicoResponse de(PreGuia preGuia) {
        return new PreGuiaMedicoResponse(
                preGuia.getIdPreGuia(),
                String.format("%04d", preGuia.getIdPreGuia()), // mesmo numero do MinhaPreGuiaResponse
                preGuia.getStatus().name(),
                preGuia.getDataCriacao());
    }
}
