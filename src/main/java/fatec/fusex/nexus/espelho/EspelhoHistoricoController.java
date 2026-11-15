package fatec.fusex.nexus.espelho;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.espelho.dto.EspelhoResponse;

@RestController
@RequestMapping("/ocs/espelho")
public class EspelhoHistoricoController {

    private final EspelhoHistoricoService service;

    public EspelhoHistoricoController(EspelhoHistoricoService service) {
        this.service = service;
    }

    @GetMapping("/historico")
    public ResponseEntity<List<EspelhoResponse>> listarEnviados() {
        return ResponseEntity.ok(service.listarEnviados());
    }
}