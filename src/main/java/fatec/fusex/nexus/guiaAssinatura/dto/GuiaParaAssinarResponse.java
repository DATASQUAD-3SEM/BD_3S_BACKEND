package fatec.fusex.nexus.guiaAssinatura.dto;

import java.util.List;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.guia.Guia;

/**
 * Item da lista de Guias para assinar (T5-BE1), com os papeis que ainda faltam assinar (pendencias).
 */
public record GuiaParaAssinarResponse(
        Long guiaId,
        String numero,
        String beneficiarioNome,
        List<AssinaturaGuiaPapel> pendencias) {

    public static GuiaParaAssinarResponse de(Guia guia, List<AssinaturaGuiaPapel> pendencias) {
        return new GuiaParaAssinarResponse(
                guia.getIdGuia(),
                guia.getNumero(),
                guia.getPreGuia().getBeneficiario().getNome(),
                pendencias);
    }
}