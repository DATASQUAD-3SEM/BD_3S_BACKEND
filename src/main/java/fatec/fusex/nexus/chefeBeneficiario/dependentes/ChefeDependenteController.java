package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** T31-BE1 (SCRUM-270): dependentes do titular na visao do chefe. */
@RestController
@RequestMapping("/chefe/beneficiarios/{id}/dependentes")
public class ChefeDependenteController {

    private final ChefeDependenteService service;
    private final ChefeDependenteCadastroService cadastroService;

    public ChefeDependenteController(ChefeDependenteService service, ChefeDependenteCadastroService cadastroService) {
        this.cadastroService = cadastroService;
        this.service = service;
    }

    @GetMapping
    public DependentesDoTitularResponse listar(@PathVariable Long id) {
        return service.listarDoTitular(id);
    }

    @PostMapping
    public ResponseEntity<DependenteItemResponse> criar(@PathVariable Long id, @RequestBody CriarDependenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroService.criar(id, request));
    }

    /** T31-BE2: conta existente para o CPF digitado no formulario do dependente (so leitura, nada e gravado). */
    @GetMapping("/conta")
    public ContaPorCpfResponse consultarConta(@PathVariable Long id, @RequestParam String cpf) {
        return cadastroService.consultarConta(cpf);
    }

    @GetMapping("/proximo-prec-cp")
    public ProximoPrecCpResponse proximoPrecCp(@PathVariable Long id) {
        return service.proximoPrecCp(id);
    }
}
