package fatec.fusex.nexus.guiaAssinatura.dto;

import org.springframework.core.io.Resource;

public record PdfGuiaAssinada(Resource arquivo, String nomeArquivo, boolean anexo) {
}