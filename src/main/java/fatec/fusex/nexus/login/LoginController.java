package fatec.fusex.nexus.login;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.login.dto.LoginRequest;
import fatec.fusex.nexus.login.dto.LoginResponse;

/**
 * POST /auth/login: confere o login (CPF ou nome de usuario) e a senha e devolve o perfil da conta.
 * O frontend usa o perfil para liberar somente as telas daquele tipo de usuario.
 */
@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public LoginResponse entrar(@RequestBody LoginRequest request) {
        return loginService.entrar(request);
    }
}
