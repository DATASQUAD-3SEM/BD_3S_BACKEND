package fatec.fusex.nexus.chefeFusex.dto;

import java.util.List;

/** Pagina da lista de medicos do chefe (T8-BE1), no mesmo formato da fila do setor de Guias. */
public record MedicoChefePaginaResponse(
        long total,
        int pagina,
        int tamanho,
        List<MedicoChefeItemResponse> itens) {
}
