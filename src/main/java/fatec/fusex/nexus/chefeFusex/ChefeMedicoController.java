package fatec.fusex.nexus.chefeFusex;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeFusex.dto.MedicoChefePaginaResponse;

//T8-BE1: GET /chefe/medicos?busca=&situacao=ativos|inativos|todos&page=&size=
/**
 * GET /chefe/medicos (T8-BE1): lista paginada de medicos com busca e filtro de situacao.
 */
@RestController
@RequestMapping("/chefe/medicos")
public class ChefeMedicoController {

    private final ChefeMedicoService chefeMedicoService;

    public ChefeMedicoController(ChefeMedicoService chefeMedicoService) {
        this.chefeMedicoService = chefeMedicoService;
    }

    @GetMapping
    public MedicoChefePaginaResponse listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String situacao,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return chefeMedicoService.listar(busca, situacao, page, size);
    }
}
