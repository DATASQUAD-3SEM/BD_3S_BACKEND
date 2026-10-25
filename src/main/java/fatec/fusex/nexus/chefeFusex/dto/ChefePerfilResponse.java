package fatec.fusex.nexus.chefeFusex.dto;

import fatec.fusex.nexus.chefeFusex.ChefeFusex;

/**
 * Resposta do GET /chefe/me, sem expor a senha.
 */
public record ChefePerfilResponse(
        String nome,
        String patente,
        String login,
        boolean assinaturaCadastrada) {

    public static ChefePerfilResponse de(ChefeFusex chefe) {
        String assinaturaUrl = chefe.getUsuario().getAssinaturaUrl();

        return new ChefePerfilResponse(
                chefe.getNome(),
                chefe.getPatente(),
                chefe.getUsuario().getLogin(),
                assinaturaUrl != null && !assinaturaUrl.isBlank()
        );
    }
}