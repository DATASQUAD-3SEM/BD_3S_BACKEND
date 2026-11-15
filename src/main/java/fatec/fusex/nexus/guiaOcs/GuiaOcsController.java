package fatec.fusex.nexus.guiaOcs;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.guiaOcs.dto.GuiaOcsResponse;

/**
 * T19-BE1: GET /ocs/guias, lista das guias recebidas pela OCS logada.
 */
@RestController
@RequestMapping("/ocs/guias")
public class GuiaOcsController {

    private final GuiaOcsService service;

    public GuiaOcsController(GuiaOcsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GuiaOcsResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
