package fatec.fusex.nexus.auditoria.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;

/**
 * Detalhe da fatura para a auditoria (T26-BE1): os campos do detalhe da fatura e, por Guia que foi a OCS em alguma
 * rodada, o andamento da glosa (T26-BE4, regra 13.2). Fatura sem glosa: andamentoGlosa vazio.
 */
public record AuditoriaDetalheResponse(
        @JsonUnwrapped FaturaDetalheResponse fatura,
        List<AndamentoGlosaResponse> andamentoGlosa) {
}
