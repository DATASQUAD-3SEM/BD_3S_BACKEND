package fatec.fusex.nexus.chefeFusex;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeFusex.dto.EditarMedicoRequest;
import fatec.fusex.nexus.chefeFusex.dto.MedicoChefeDetalheResponse;

//T8-BE3: GET e PUT /chefe/medicos/{id}
@RestController
@RequestMapping("/chefe/medicos")
public class ChefeMedicoEdicaoController {

    private final ChefeMedicoEdicaoService edicaoService;

    public ChefeMedicoEdicaoController(ChefeMedicoEdicaoService edicaoService) {
        this.edicaoService = edicaoService;
    }

    @GetMapping("/{id}")
    public MedicoChefeDetalheResponse buscar(@PathVariable Long id) {
        return edicaoService.buscar(id);
    }

    @PutMapping("/{id}")
    public MedicoChefeDetalheResponse editar(@PathVariable Long id, @RequestBody EditarMedicoRequest request) {
        return edicaoService.editar(id, request);
    }
}
