package fatec.fusex.nexus.setorFatura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EtapaFluxo(String etapa,
                         String setor,
                         LocalDateTime dataEntrada,
                         LocalDateTime dataSaida,
                         String responsavel,
                         BigDecimal valorInicial,
                         BigDecimal valorFinal) {
}