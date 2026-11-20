package fatec.fusex.nexus.rastreabilidade.dto;

import java.math.BigDecimal;

public record ItemLisuraRequest(Long itemId,
                                Boolean conforme,
                                BigDecimal valorContrato,
                                Boolean corrigirTabela,
                                String observacao) {
}