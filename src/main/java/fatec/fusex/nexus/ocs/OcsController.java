package fatec.fusex.nexus.ocs;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.ocs.dto.OcsResponse;

/**
 * SCRUM-29b: endpoint de consulta de OCS. A decisao de fonte dos dados
 * esta em docs/SPIKE_OCS.md (SCRUM-29a): cadastro interno no banco.
 *
 * Contrato (docs/CONTRATO_API.md):
 *   GET /ocs -> Ocs[]
 * O front chama /api/ocs (o http.ts coloca o prefixo /api; o Vite remove).
 */
@RestController
@RequestMapping("/ocs")
public class OcsController {

    private final OcsService ocsService;

    public OcsController(OcsService ocsService) {
        this.ocsService = ocsService;
    }

    @GetMapping
    public List<OcsResponse> listar() {
        return ocsService.listar().stream()
                .map(OcsResponse::de)
                .toList();
    }
}