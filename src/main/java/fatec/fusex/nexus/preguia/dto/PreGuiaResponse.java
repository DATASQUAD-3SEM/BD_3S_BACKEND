package fatec.fusex.nexus.preguia.dto;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preguia.PreGuia;

/**
 * Nunca devolvemos a entidade PreGuia direto (ARQUITETURA.md).
 * IMPORTANTE: monte este DTO AINDA DENTRO do metodo @Transactional do Service.
 */
public record PreGuiaResponse(
        Long id,
        String status,
        LocalDateTime dataEmissao,
        String encaminhamentoUrl,
        Long ocsId) {

    public static PreGuiaResponse de(PreGuia preGuia) {
        return new PreGuiaResponse(
                preGuia.getId(),
                preGuia.getStatus().name(),
                preGuia.getDataEmissao(),
                preGuia.getEncaminhamentoUrl(),
                preGuia.getOcs().getId());
    }
}