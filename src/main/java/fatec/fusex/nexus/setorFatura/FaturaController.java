package fatec.fusex.nexus.setorFatura;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.setorFatura.dto.FaturaResumoResponse;

@RestController
@RequestMapping("/setor-fatura/faturas")
public class FaturaController {

    private final FaturaConsultaService service;

    public FaturaController(FaturaConsultaService service) {
        this.service = service;
    }

    @GetMapping
    public List<FaturaResumoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public FaturaDetalheResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }
}