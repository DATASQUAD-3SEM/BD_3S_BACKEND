package fatec.fusex.nexus.chefeFusex;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeFusex.dto.CadastrarMedicoRequest;
import fatec.fusex.nexus.chefeFusex.dto.MedicoCadastradoResponse;

//T8-BE2: POST /chefe/medicos (a listagem GET /chefe/medicos fica no ChefeMedicoController, da T8-BE1)
@RestController
@RequestMapping("/chefe/medicos")
public class ChefeMedicoCadastroController {

    private final ChefeMedicoCadastroService cadastroService;

    public ChefeMedicoCadastroController(ChefeMedicoCadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @PostMapping
    public ResponseEntity<MedicoCadastradoResponse> cadastrar(@RequestBody CadastrarMedicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cadastroService.cadastrar(request));
    }
}
