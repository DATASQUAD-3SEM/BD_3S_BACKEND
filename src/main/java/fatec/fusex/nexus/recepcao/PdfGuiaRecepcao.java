package fatec.fusex.nexus.recepcao;

import org.springframework.core.io.Resource;

/**
 * PDF da Guia entregue a recepcao, com o nome do arquivo e se deve ser baixado (anexo).
 */
public record PdfGuiaRecepcao(Resource arquivo, String nomeArquivo, boolean anexo) {
}