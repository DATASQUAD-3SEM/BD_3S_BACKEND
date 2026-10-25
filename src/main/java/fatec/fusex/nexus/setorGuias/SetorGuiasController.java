package fatec.fusex.nexus.setorGuias;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorGuias.dto.DetalhePreGuiaResponse;
import fatec.fusex.nexus.setorGuias.dto.FilaPreGuiaPaginaResponse;

@RestController
@RequestMapping("/setor-guias")
public class SetorGuiasController {

    private final SetorGuiasService setorGuiasService;

    public SetorGuiasController(SetorGuiasService setorGuiasService) {
        this.setorGuiasService = setorGuiasService;
    }

    @GetMapping("/pre-guias")
    public FilaPreGuiaPaginaResponse listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return setorGuiasService.listar(busca, status, page, size);
    }

    @GetMapping("/pre-guias/{id}")
    public DetalhePreGuiaResponse detalhar(@PathVariable Long id) {
        return setorGuiasService.detalhar(id);
    }
}