package fatec.fusex.nexus.usuario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssinaturaBase64Request(String imagemBase64) {

    @Override
    public String toString() {
        return "AssinaturaBase64Request[imagemBase64=oculto]";
    }
}