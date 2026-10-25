package fatec.fusex.nexus.minhasPreGuias.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record EncaminhamentoArquivo(
        Resource recurso,
        MediaType mediaType,
        String filename
) {}