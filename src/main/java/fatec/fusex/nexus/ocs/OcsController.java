package fatec.fusex.nexus.ocs;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocs")
public class OcsController {

    private final OcsService ocsService;

    public OcsController(OcsService ocsService) {
        this.ocsService = ocsService;
    }

    @GetMapping
    public List<Ocs> listar(
            @RequestParam(required = false) String busca
    ) {
        if (busca == null || busca.isBlank()) {
            return ocsService.listar();
        }

        return ocsService.buscarPorNome(busca);
    }
}
