package fatec.fusex.nexus.espelho;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.espelho.dto.EspelhoResponse;
import fatec.fusex.nexus.espelho.dto.ValorProcedimentoRequest;

@RestController
@RequestMapping("/ocs/espelho/guias")
public class EspelhoGuiaController {

    private final EspelhoGuiaService service;

    public EspelhoGuiaController(EspelhoGuiaService service) {
        this.service = service;
    }

    @PostMapping("/{guiaId}")
    public ResponseEntity<EspelhoResponse> adicionarGuia(@PathVariable("guiaId") Long guiaId,
                                                         @RequestBody List<ValorProcedimentoRequest> valores) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarGuia(guiaId, valores));
    }
}