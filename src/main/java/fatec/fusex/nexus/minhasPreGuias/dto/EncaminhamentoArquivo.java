package fatec.fusex.nexus.minhasPreGuias.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * Arquivo de encaminhamento pronto para ser devolvido na resposta, com o tipo de conteudo correto.
 */
public record EncaminhamentoArquivo(
        Resource recurso,
        MediaType mediaType,
        String filename
) {}