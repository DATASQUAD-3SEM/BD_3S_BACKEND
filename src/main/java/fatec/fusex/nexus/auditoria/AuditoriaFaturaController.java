package fatec.fusex.nexus.auditoria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.auditoria.dto.AuditoriaAnalisadaResponse;
import fatec.fusex.nexus.auditoria.dto.AuditoriaDetalheResponse;
import fatec.fusex.nexus.auditoria.dto.AuditoriaResumoResponse;
import fatec.fusex.nexus.rastreabilidade.dto.EnviarFaturaRequest;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;

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

    @GetMapping("/analisadas")
    public List<AuditoriaAnalisadaResponse> analisadas() {
        return service.analisadas();
    }

    @GetMapping("/{id}")
    public AuditoriaDetalheResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @PostMapping("/{id}/visualizar")
    public FaturaDetalheResponse visualizar(@PathVariable Long id, @RequestBody EnviarFaturaRequest request) {
        return service.visualizar(id, request);
    }
}
