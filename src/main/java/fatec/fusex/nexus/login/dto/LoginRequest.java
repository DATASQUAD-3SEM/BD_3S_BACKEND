package fatec.fusex.nexus.login.dto;

/**
 * Corpo do POST /auth/login. O campo login aceita o CPF (com ou sem pontos e traco) ou o nome de usuario.
 * O toString omite a senha para que ela nunca apareca em log.
 */
public record LoginRequest(String login, String senha) {

    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", senha=***]";
    }
}
