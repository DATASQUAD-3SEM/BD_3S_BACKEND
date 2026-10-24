package fatec.fusex.nexus.guiaAssinatura.dto;

import java.util.List;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.guia.Guia;

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