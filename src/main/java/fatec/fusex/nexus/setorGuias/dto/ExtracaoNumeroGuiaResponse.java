package fatec.fusex.nexus.setorGuias.dto;

/**
 * Resposta do T11-BE1: POST /setor-guias/extrair-numero-guia. Devolve so o numero lido do PDF; nada e gravado.
 */
public record ExtracaoNumeroGuiaResponse(String numeroGuia) {
}
