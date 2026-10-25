package fatec.fusex.nexus.chefeBeneficiario.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record BeneficiarioListaResponse(
        long total, int pagina, int tamanho, List<BeneficiarioItemResponse> itens) {

    public static BeneficiarioListaResponse de(Page<BeneficiarioItemResponse> page) {
        return new BeneficiarioListaResponse(
                page.getTotalElements(), page.getNumber(), page.getSize(), page.getContent());
    }
}