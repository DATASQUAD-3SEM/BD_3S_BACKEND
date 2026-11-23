package fatec.fusex.nexus.guiaAssinatura.dto;

import java.util.List;

public record AssinarLoteResponse(List<Long> assinadas, List<FalhaAssinaturaLote> falhas) {
}
