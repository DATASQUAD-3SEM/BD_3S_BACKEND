package fatec.fusex.nexus.setorFatura;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorFatura.dto.FaturaAuditadaResponse;

@RestController
@RequestMapping("/setor-fatura")
public class FaturaAuditadaController {

    private final FaturaConsultaService service;

    public FaturaAuditadaController(FaturaConsultaService service) {
        this.service = service;
    }

    @GetMapping("/faturas-auditadas")
    public List<FaturaAuditadaResponse> auditadas() {
        return service.listarAuditadas();
    }
}