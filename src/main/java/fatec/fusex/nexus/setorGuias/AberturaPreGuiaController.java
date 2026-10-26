package fatec.fusex.nexus.setorGuias;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorGuias.dto.AberturaPreGuiaResponse;

//T4-BE3: POST /setor-guias/pre-guias/{id}/abertura, sem corpo
/**
 * POST /setor-guias/pre-guias/{id}/abertura (T4-BE3): marca a pre-guia como Em analise ao ser aberta.
 */
@RestController
@RequestMapping("/setor-guias/pre-guias")
public class AberturaPreGuiaController {

    private final AberturaPreGuiaService aberturaService;

    public AberturaPreGuiaController(AberturaPreGuiaService aberturaService) {
        this.aberturaService = aberturaService;
    }

    @PostMapping("/{id}/abertura")
    public AberturaPreGuiaResponse abrir(@PathVariable Long id) {
        return aberturaService.abrir(id);
    }
}
