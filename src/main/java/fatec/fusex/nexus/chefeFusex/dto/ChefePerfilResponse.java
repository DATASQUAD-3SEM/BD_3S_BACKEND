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
        return new ChefePerfilResponse(
                chefe.getNome(),
                chefe.getPatente(),
                chefe.getUsuario().getLogin(),
                chefe.getUsuario().getAssinatura() != null
        );
    }
}