package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** T31-BE1 (SCRUM-270): dependentes do titular na visao do chefe. */
@RestController
@RequestMapping("/chefe/beneficiarios/{id}/dependentes")
public class ChefeDependenteController {

    private final ChefeDependenteService service;

    public ChefeDependenteController(ChefeDependenteService service) {
        this.service = service;
    }

    @GetMapping
    public DependentesDoTitularResponse listar(@PathVariable Long id) {
        return service.listarDoTitular(id);
    }

    @GetMapping("/proximo-prec-cp")
    public ProximoPrecCpResponse proximoPrecCp(@PathVariable Long id) {
        return service.proximoPrecCp(id);
    }
}
