package fatec.fusex.nexus.setorGuias.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record FilaPreGuiaPaginaResponse(
        long total,
        int pagina,
        int tamanho,
        List<FilaPreGuiaItemResponse> itens) {

    public static FilaPreGuiaPaginaResponse de(Page<FilaPreGuiaItemResponse> pagina) {
        return new FilaPreGuiaPaginaResponse(
                pagina.getTotalElements(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getContent());
    }
}