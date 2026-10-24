package fatec.fusex.nexus.MinhasPreGuias.dto;

import org.springframework.core.io.Resource;

public record GuiaPdf(Resource resource, String filename, boolean inline) {
}