package fatec.fusex.nexus.chefeBeneficiario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioCadastradoResponse;
import fatec.fusex.nexus.chefeBeneficiario.dto.CadastrarBeneficiarioRequest;

//T7-BE2: POST /chefe/beneficiarios (a listagem GET fica no ChefeBeneficiarioController, da T7-BE1)
/**
 * POST /chefe/beneficiarios (T7-BE2): cadastro de beneficiario pelo Chefe. Devolve 201 com o id criado.
 */
@RestController
@RequestMapping("/chefe/beneficiarios")
public class ChefeBeneficiarioCadastroController {

    private final ChefeBeneficiarioCadastroService cadastroService;

    public ChefeBeneficiarioCadastroController(ChefeBeneficiarioCadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @PostMapping
    public ResponseEntity<BeneficiarioCadastradoResponse> cadastrar(
            @RequestBody(required = false) CadastrarBeneficiarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroService.cadastrar(request));
    }
}
