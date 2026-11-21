package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** T31-BE3 (SCRUM-272): PUT /chefe/dependentes/{id}. */
@RestController
@RequestMapping("/chefe/dependentes/{id}")
public class ChefeDependenteEdicaoController {

    private final ChefeDependenteCadastroService cadastroService;

    public ChefeDependenteEdicaoController(ChefeDependenteCadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @PutMapping
    public DependenteItemResponse editar(@PathVariable Long id, @RequestBody EditarDependenteRequest request) {
        return cadastroService.editar(id, request);
    }
}
