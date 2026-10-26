package fatec.fusex.nexus.usuario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo do PUT /usuarios/me/assinatura quando a assinatura vem desenhada no quadro (imagem em base64).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssinaturaBase64Request(String imagemBase64) {

    @Override
    public String toString() {
        return "AssinaturaBase64Request[imagemBase64=oculto]";
    }
}