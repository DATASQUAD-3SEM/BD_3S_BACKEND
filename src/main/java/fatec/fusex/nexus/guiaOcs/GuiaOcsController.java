package fatec.fusex.nexus.guiaOcs;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.guiaOcs.dto.GuiaOcsDetalheResponse;
import fatec.fusex.nexus.guiaOcs.dto.GuiaOcsResponse;

/**
 * T19-BE1: GET /ocs/guias, lista das guias recebidas pela OCS logada.
 * T19-BE2: GET /ocs/guias/{id}, modelo digitalizado de uma dessas guias.
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

    @GetMapping("/{id}")
    public ResponseEntity<GuiaOcsDetalheResponse> detalhar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.detalhar(id));
    }

    /** T19-BE3: requer a assinatura de uma sessao da guia (uma por requisicao, sem corpo). */
    @PostMapping("/{id}/sessoes/{sessaoId}/solicitacao")
    public ResponseEntity<GuiaOcsDetalheResponse> solicitarAssinatura(@PathVariable("id") Long id,
                                                                      @PathVariable("sessaoId") Long sessaoId) {
        return ResponseEntity.ok(service.solicitarAssinatura(id, sessaoId));
    }
}
