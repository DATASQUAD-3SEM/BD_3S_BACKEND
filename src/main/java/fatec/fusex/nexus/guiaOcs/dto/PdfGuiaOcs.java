package fatec.fusex.nexus.guiaOcs.dto;

/**
 * PDF da guia entregue a OCS (T19-BE4 e T23-BE2): bytes montados e o nome do arquivo para o cabecalho.
 */
public record PdfGuiaOcs(byte[] conteudo, String nomeArquivo) {
}
