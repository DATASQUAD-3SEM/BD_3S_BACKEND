package fatec.fusex.nexus.versoGuia;

import java.math.BigDecimal;
import java.util.List;

import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse;

/** Dados de uma pagina de verso (T32-BE1): uma Guia glosada da fatura. */
record VersoGuia(
        Long guiaId,
        String guiaNumero,
        String numeroFatura,
        String paciente,
        String titular,
        String ocs,
        String contrato,
        List<Linha> linhas,
        BigDecimal totalGlosa,
        List<String> observacoes,
        AndamentoGlosaResponse andamento) {

    record Linha(String codigo, String procedimento, int quantidade, BigDecimal informado, BigDecimal contrato,
                 BigDecimal glosa) {
    }
}
