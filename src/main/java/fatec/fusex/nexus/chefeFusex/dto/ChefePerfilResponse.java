package fatec.fusex.nexus.chefeFusex.dto;

import fatec.fusex.nexus.chefeFusex.ChefeFusex;

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