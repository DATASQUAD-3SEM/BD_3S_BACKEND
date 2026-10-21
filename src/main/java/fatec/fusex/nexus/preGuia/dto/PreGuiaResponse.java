package fatec.fusex.nexus.preGuia.dto;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;

/**
 * DTO de saida da pre-guia. Monte este DTO dentro do metodo @Transactional do Service,
 * para nao acessar associacoes LAZY fora da transacao.
 */
public record PreGuiaResponse(
        Long id,
        String status,
        LocalDateTime dataEmissao,
        String encaminhamentoUrl,
        Long ocsId) {

    public static PreGuiaResponse de(PreGuia preGuia) {
        return new PreGuiaResponse(
                preGuia.getIdPreGuia(),
                preGuia.getStatus().name(),
                preGuia.getDataCriacao(),
                preGuia.getEncaminhamentoUrl(),
                preGuia.getOcs().getIdOcs());
    }
}