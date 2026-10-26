package fatec.fusex.nexus.guiaAssinatura.dto;

import java.util.List;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;

/**
 * Resposta do POST /guias/{id}/assinaturas: papeis ja assinados e se a Guia ficou concluida.
 */
public record AssinaturaRegistradaResponse(
        List<AssinaturaGuiaPapel> assinaturas,
        boolean concluida) {
}