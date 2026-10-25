package fatec.fusex.nexus.usuario.dto;

/**
 * Resposta do POST /usuarios/me/senha, sem expor nenhuma senha.
 */
public record SenhaAlteradaResponse(boolean senhaAlterada) {
}