package fatec.fusex.nexus.setorFatura;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.setorFatura.dto.FaturaResumoResponse;
import fatec.fusex.nexus.setorFatura.dto.ProtocolarFaturaRequest;
import fatec.fusex.nexus.setorFatura.dto.ProximoNumeroFaturaResponse;

@RestController
@RequestMapping("/setor-fatura/faturas")
public class FaturaController {

    private final FaturaConsultaService service;
    private final FaturaProtocoloService protocoloService;

    public FaturaController(FaturaConsultaService service, FaturaProtocoloService protocoloService) {
        this.service = service;
        this.protocoloService = protocoloService;
    }

    @GetMapping
    public List<FaturaResumoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public FaturaDetalheResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @GetMapping("/{id}/proximo-numero")
    public ProximoNumeroFaturaResponse proximoNumero(@PathVariable Long id) {
        return protocoloService.sugerirProximoNumero(id);
    }

    @PostMapping("/{id}/protocolo")
    public FaturaDetalheResponse protocolar(@PathVariable Long id, @RequestBody ProtocolarFaturaRequest request) {
        return protocoloService.protocolar(id, request);
    }
}