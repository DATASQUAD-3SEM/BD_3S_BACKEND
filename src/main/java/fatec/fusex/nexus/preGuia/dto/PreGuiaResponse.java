package fatec.fusex.nexus.preGuia.dto;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;

/**
 * Resposta do POST /pre-guias. O numero tem 4 digitos, igual ao exibido em Minhas pre-guias.
 * Monte este DTO dentro do metodo @Transactional do Service, para nao acessar associacoes LAZY fora da transacao.
 */
public record PreGuiaResponse(
        Long id,
        String numero,
        String status,
        LocalDateTime dataCriacao,
        Long ocsId) {

    public static PreGuiaResponse de(PreGuia preGuia) {
        return new PreGuiaResponse(
                preGuia.getIdPreGuia(),
                String.format("%04d", preGuia.getIdPreGuia()),
                preGuia.getStatus().name(),
                preGuia.getDataCriacao(),
                preGuia.getOcs().getIdOcs());
    }
}
