package fatec.fusex.nexus.setorGuias.dto;

/** Resposta do PUT /setor-guias/pre-guias/{id}/guia-rascunho (T4-BE5). */
public record GuiaRascunhoResponse(String numero, String nomeArquivo) {
}
