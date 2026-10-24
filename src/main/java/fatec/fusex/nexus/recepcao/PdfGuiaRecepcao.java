package fatec.fusex.nexus.recepcao;

import org.springframework.core.io.Resource;

public record PdfGuiaRecepcao(Resource arquivo, String nomeArquivo, boolean anexo) {
}