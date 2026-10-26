package fatec.fusex.nexus.minhasPreGuias.dto;

import org.springframework.core.io.Resource;

/**
 * PDF da Guia devolvido ao beneficiario (T1-BE2), indicando se deve ser exibido (inline) ou baixado.
 */
public record GuiaPdf(Resource resource, String filename, boolean inline) {
}