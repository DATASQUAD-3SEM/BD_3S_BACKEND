package fatec.fusex.nexus.andamentoGlosa.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Andamento da glosa de uma Guia (T26-BE4, regra 13.2): o selo final e os itens em ordem de data.
 * situacaoFinal: ACEITA_PELA_OCS, PRAZO_VENCIDO, CONTESTACAO_ACATADA ou EM_ANDAMENTO.
 */
public record AndamentoGlosaResponse(
        Long guiaId,
        String guiaNumero,
        String situacaoFinal,
        String seloFinal,
        List<ItemAndamento> itens) {

    public record ItemAndamento(LocalDateTime data, String texto) {
    }
}
