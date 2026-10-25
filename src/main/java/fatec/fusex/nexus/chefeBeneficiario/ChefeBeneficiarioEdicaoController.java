package fatec.fusex.nexus.chefeBeneficiario;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioDetalheResponse;
import fatec.fusex.nexus.chefeBeneficiario.dto.EditarBeneficiarioRequest;

//T7-BE3: GET e PUT /chefe/beneficiarios/{id}
/**
 * GET e PUT /chefe/beneficiarios/{id} (T7-BE3): leitura e edicao de um beneficiario pelo Chefe.
 */
@RestController
@RequestMapping("/chefe/beneficiarios")
public class ChefeBeneficiarioEdicaoController {

    private final ChefeBeneficiarioEdicaoService edicaoService;

    public ChefeBeneficiarioEdicaoController(ChefeBeneficiarioEdicaoService edicaoService) {
        this.edicaoService = edicaoService;
    }

    @GetMapping("/{id}")
    public BeneficiarioDetalheResponse buscar(@PathVariable Long id) {
        return edicaoService.buscar(id);
    }

    @PutMapping("/{id}")
    public BeneficiarioDetalheResponse editar(@PathVariable Long id,
                                              @RequestBody(required = false) EditarBeneficiarioRequest request) {
        return edicaoService.editar(id, request);
    }
}
