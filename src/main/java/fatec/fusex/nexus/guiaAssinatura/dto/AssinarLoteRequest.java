package fatec.fusex.nexus.guiaAssinatura.dto;

import java.util.List;

public record AssinarLoteRequest(List<Long> guiaIds) {
}
