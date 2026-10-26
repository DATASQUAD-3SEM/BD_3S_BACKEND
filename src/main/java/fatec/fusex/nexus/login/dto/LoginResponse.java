package fatec.fusex.nexus.login.dto;

/**
 * Resposta do POST /auth/login: login gravado (CPF so com digitos ou nome de usuario), perfil e nome exibido.
 * Nunca expoe a senha.
 */
public record LoginResponse(String login, String perfil, String nome) {
}
