package fatec.fusex.nexus.guiaAssinatura.dto;

import org.springframework.core.io.Resource;

/**
 * PDF da Guia ja com as assinaturas aplicadas, com o nome do arquivo e se deve ser baixado (anexo) ou exibido.
 */
public record PdfGuiaAssinada(Resource arquivo, String nomeArquivo, boolean anexo) {
}