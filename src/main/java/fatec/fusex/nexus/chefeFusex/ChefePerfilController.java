package fatec.fusex.nexus.chefeFusex;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;

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
}