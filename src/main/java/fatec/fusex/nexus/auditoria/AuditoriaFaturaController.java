package fatec.fusex.nexus.auditoria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.auditoria.dto.AuditoriaDetalheResponse;
import fatec.fusex.nexus.auditoria.dto.AuditoriaResumoResponse;

@RestController
@RequestMapping("/auditoria/faturas")
public class AuditoriaFaturaController {

    private final AuditoriaFaturaService service;

    public AuditoriaFaturaController(AuditoriaFaturaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditoriaResumoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AuditoriaDetalheResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }
}
