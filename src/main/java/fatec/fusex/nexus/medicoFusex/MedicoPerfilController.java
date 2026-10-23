package fatec.fusex.nexus.medicoFusex;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.medicoFusex.dto.MedicoPerfilResponse;

@RestController
@RequestMapping("/medicos")
public class MedicoPerfilController {

    private final MedicoPerfilService medicoPerfilService;

    public MedicoPerfilController(
            MedicoPerfilService medicoPerfilService) {
        this.medicoPerfilService = medicoPerfilService;
    }

    @GetMapping("/me/perfil")
    public MedicoPerfilResponse meuPerfil() {
        return medicoPerfilService.buscarPerfilLogado();
    }
}