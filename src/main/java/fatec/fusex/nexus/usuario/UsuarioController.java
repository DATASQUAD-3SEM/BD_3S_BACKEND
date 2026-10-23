package fatec.fusex.nexus.usuario;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.usuario.dto.AlterarSenhaRequest;
import fatec.fusex.nexus.usuario.dto.SenhaAlteradaResponse;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/me/senha")
    public SenhaAlteradaResponse alterarSenha(
            @RequestBody AlterarSenhaRequest request) {

        usuarioService.alterarSenha(request);

        return new SenhaAlteradaResponse(true);
    }
}