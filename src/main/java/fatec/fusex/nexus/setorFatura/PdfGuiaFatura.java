package fatec.fusex.nexus.setorFatura;

import org.springframework.core.io.Resource;

public record PdfGuiaFatura(Resource arquivo, String nomeArquivo) {
}