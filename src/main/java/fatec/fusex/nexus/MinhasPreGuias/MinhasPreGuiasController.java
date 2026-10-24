package fatec.fusex.nexus.MinhasPreGuias;

import fatec.fusex.nexus.MinhasPreGuias.dto.MinhaPreGuiaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/beneficiarios/me/pre-guias")
public class MinhasPreGuiasController {

    private final MinhasPreGuiasService minhasPreGuiasService;

    public MinhasPreGuiasController(MinhasPreGuiasService minhasPreGuiasService) {
        this.minhasPreGuiasService = minhasPreGuiasService;
    }

    @GetMapping
    public ResponseEntity<List<MinhaPreGuiaResponse>> listar() {
        List<MinhaPreGuiaResponse> response = minhasPreGuiasService.listar();
        return ResponseEntity.ok(response);
    }
}