package fatec.fusex.nexus.espelho;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.espelho.dto.EspelhoResponse;
import fatec.fusex.nexus.espelho.dto.ValorProcedimentoRequest;

@RestController
@RequestMapping("/ocs/espelho/guias")
public class EspelhoEdicaoController {

    private final EspelhoEdicaoService service;

    public EspelhoEdicaoController(EspelhoEdicaoService service) {
        this.service = service;
    }

    @PutMapping("/{guiaId}")
    public ResponseEntity<EspelhoResponse> editarValores(@PathVariable("guiaId") Long guiaId,
                                                         @RequestBody List<ValorProcedimentoRequest> valores) {
        return ResponseEntity.ok(service.editarValores(guiaId, valores));
    }
}