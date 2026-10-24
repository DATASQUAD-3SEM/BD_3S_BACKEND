package fatec.fusex.nexus.preGuia.dto;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;

/**
 * DTO de saida da pre-guia. Monte este DTO dentro do metodo @Transactional do Service,
 * para nao acessar associacoes LAZY fora da transacao.
 */
public record PreGuiaResponse(
        Long id,
        String numero,
        String origem,
        String status,
        LocalDateTime dataEmissao,
        String observacao,
        LocalDateTime dataRegistroObservacao,
        String encaminhamentoNome,
        String encaminhamentoTipo,
        String encaminhamentoUrl,
        LocalDateTime dataEnvioEncaminhamento,
        Long ocsId) {

    public static PreGuiaResponse de(PreGuia preGuia) {
        return new PreGuiaResponse(
                preGuia.getIdPreGuia(),
                preGuia.getNumero(),
                preGuia.getOrigem() != null ? preGuia.getOrigem().name() : null,
                preGuia.getStatus().name(),
                preGuia.getDataCriacao(),
                preGuia.getObservacao(),
                preGuia.getDataRegistroObservacao(),
                preGuia.getEncaminhamentoNome(),
                preGuia.getEncaminhamentoTipo(),
                preGuia.getEncaminhamentoUrl(),
                preGuia.getDataEnvioEncaminhamento(),
                preGuia.getOcs().getIdOcs());
    }
}