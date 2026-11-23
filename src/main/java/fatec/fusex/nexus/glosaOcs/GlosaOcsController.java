package fatec.fusex.nexus.glosaOcs;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.glosaOcs.dto.DecisaoGlosaRequest;
import fatec.fusex.nexus.glosaOcs.dto.GlosaDetalheResponse;
import fatec.fusex.nexus.glosaOcs.dto.GlosaResumoResponse;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;

@RestController
@RequestMapping("/ocs/glosas")
public class GlosaOcsController {

    private final GlosaOcsService service;

    public GlosaOcsController(GlosaOcsService service) {
        this.service = service;
    }

    @GetMapping
    public List<GlosaResumoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public GlosaDetalheResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @PostMapping("/{id}/decisao")
    public FaturaDetalheResponse decidir(@PathVariable Long id, @RequestBody DecisaoGlosaRequest request) {
        return service.decidir(id, request);
    }
}
