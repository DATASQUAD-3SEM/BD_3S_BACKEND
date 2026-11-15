package fatec.fusex.nexus.espelho.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValorProcedimentoRequest(
        @JsonProperty("cod_dgp") String codDgp,
        @JsonProperty("valor_sessao") BigDecimal valorSessao) {}