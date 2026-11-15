package fatec.fusex.nexus.guiaFinalizada;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.guiaFinalizada.dto.GuiaFinalizadaResponse;

@RestController
@RequestMapping("/ocs/guias-finalizadas")
public class GuiaFinalizadaController {

    private final GuiaFinalizadaService service;

    public GuiaFinalizadaController(GuiaFinalizadaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GuiaFinalizadaResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}