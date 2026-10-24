package fatec.fusex.nexus.guiaAssinatura.dto;

import java.util.List;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;

public record AssinaturaRegistradaResponse(
        List<AssinaturaGuiaPapel> assinaturas,
        boolean concluida) {
}