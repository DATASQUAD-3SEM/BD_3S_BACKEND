package fatec.fusex.nexus.chefeFusex;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;
import fatec.fusex.nexus.chefeFusex.dto.AtualizarChefeRequest;

/**
 * GET e PUT /chefe/me (T12-BE1 e T12-BE2): dados da conta do Chefe logado e sua atualizacao.
 */
@RestController
@RequestMapping("/chefe")
public class ChefePerfilController {

    private final ChefePerfilService chefePerfilService;

    public ChefePerfilController(ChefePerfilService chefePerfilService) {
        this.chefePerfilService = chefePerfilService;
    }

    @GetMapping("/me")
    public ChefePerfilResponse meuPerfil() {
        return chefePerfilService.buscarPerfilLogado();
    }

    @PutMapping("/me")
    public ChefePerfilResponse atualizarMeuPerfil(
            @Valid @RequestBody AtualizarChefeRequest request
    ) {
        return chefePerfilService.atualizarPerfil(request);
    }
}