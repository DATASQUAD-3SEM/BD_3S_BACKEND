package fatec.fusex.nexus.usuario.dto;

/**
 * Resposta do cadastro ou substituicao da assinatura digital: assinaturaCadastrada = true.
 */
public record AssinaturaAtualizadaResponse(
        boolean assinaturaCadastrada) {
}