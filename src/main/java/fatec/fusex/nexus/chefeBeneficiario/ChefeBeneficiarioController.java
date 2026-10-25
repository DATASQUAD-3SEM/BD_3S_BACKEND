package fatec.fusex.nexus.chefeBeneficiario;

import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioListaResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chefe/beneficiarios")
public class ChefeBeneficiarioController {

    private final ChefeBeneficiarioService service;

    public ChefeBeneficiarioController(ChefeBeneficiarioService service) {
        this.service = service;
    }

    @GetMapping
    public BeneficiarioListaResponse listar(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "ativos") String situacao,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(busca, situacao, page, size);
    }
}