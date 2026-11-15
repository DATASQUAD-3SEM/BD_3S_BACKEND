package fatec.fusex.nexus.espelho;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.espelho.dto.EspelhoResponse;

@RestController
@RequestMapping("/ocs/espelho/guias")
public class EspelhoRemocaoController {

    private final EspelhoRemocaoService service;

    public EspelhoRemocaoController(EspelhoRemocaoService service) {
        this.service = service;
    }

    @DeleteMapping
    public ResponseEntity<EspelhoResponse> removerGuias(@RequestBody List<Long> guiaIds) {
        return ResponseEntity.ok(service.removerGuias(guiaIds));
    }
}