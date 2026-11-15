package fatec.fusex.nexus.espelho;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.espelho.dto.EspelhoResponse;

@RestController
@RequestMapping("/ocs/espelho")
public class EspelhoController {

    private final EspelhoService service;

    public EspelhoController(EspelhoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<EspelhoResponse> consultarEmConstrucao() {
        return ResponseEntity.ok(service.consultarEmConstrucao());
    }
}