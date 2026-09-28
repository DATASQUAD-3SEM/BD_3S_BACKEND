package fatec.fusex.nexus.preguia.dto;

import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.preguia.PreGuia;
import fatec.fusex.nexus.procedimento.ProcedimentoExame;

/**
 * Nunca devolvemos a entidade PreGuia direto (ARQUITETURA.md). Os nomes de campo
 * batem com o tipo PreGuia de shared/types/domain.ts no front (docs/CONTRATO_API.md).
 *
 * IMPORTANTE: monte este DTO AINDA DENTRO do metodo @Transactional do Service (nao no
 * Controller). ocs/procedimentos sao LAZY - se voce montar o DTO depois que a transacao
 * ja fechou, preGuia.getProcedimentos() pode estourar LazyInitializationException.
 */
public record PreGuiaResponse(
        Long id,
        String status,
        LocalDateTime dataEmissao,
        String encaminhamentoUrl,
        Long ocsId,
        List<Long> procedimentoIds) {

    public static PreGuiaResponse de(PreGuia preGuia) {
        return new PreGuiaResponse(
                preGuia.getId(),
                preGuia.getStatus().name(),
                preGuia.getDataEmissao(),
                preGuia.getEncaminhamentoUrl(),
                preGuia.getOcs().getId(),
                preGuia.getProcedimentos().stream()
                        .map(ProcedimentoExame::getId)
                        .sorted()
                        .toList());
    }
}
