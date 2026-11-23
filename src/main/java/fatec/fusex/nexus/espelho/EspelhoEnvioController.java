package fatec.fusex.nexus.espelho;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.espelho.dto.EspelhoResponse;

@RestController
@RequestMapping("/ocs/espelho")
public class EspelhoEnvioController {

    private final EspelhoEnvioService service;

    public EspelhoEnvioController(EspelhoEnvioService service) {
        this.service = service;
    }

    @PostMapping("/enviar")
    public ResponseEntity<EspelhoResponse> enviar() {
        return ResponseEntity.ok(service.enviar());
    }
}