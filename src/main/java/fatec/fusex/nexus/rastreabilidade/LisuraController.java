package fatec.fusex.nexus.rastreabilidade;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.rastreabilidade.dto.EnviarFaturaRequest;
import fatec.fusex.nexus.rastreabilidade.dto.FaturaLisuraResumoResponse;
import fatec.fusex.nexus.rastreabilidade.dto.ItemLisuraRequest;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;

@RestController
@RequestMapping("/rastreabilidade/faturas")
public class LisuraController {

    private final LisuraService service;
    private final LisuraConsultaService consultaService;
    private final LisuraAuditoriaService auditoriaService;

    public LisuraController(LisuraService service,
                            LisuraConsultaService consultaService,
                            LisuraAuditoriaService auditoriaService) {
        this.service = service;
        this.consultaService = consultaService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<FaturaLisuraResumoResponse> listar() {
        return consultaService.listar();
    }

    @GetMapping("/{id}")
    public FaturaDetalheResponse detalhar(@PathVariable Long id) {
        return consultaService.detalhar(id);
    }

    @PutMapping("/{id}/lisura")
    public FaturaDetalheResponse salvar(@PathVariable Long id, @RequestBody List<ItemLisuraRequest> itens) {
        return service.salvar(id, itens);
    }

    @PostMapping("/{id}/enviar-ocs")
    public FaturaDetalheResponse enviarOcs(@PathVariable Long id, @RequestBody EnviarFaturaRequest request) {
        return service.enviarOcs(id, request);
    }

    @PostMapping("/{id}/enviar-auditoria")
    public FaturaDetalheResponse enviarAuditoria(@PathVariable Long id, @RequestBody EnviarFaturaRequest request) {
        return auditoriaService.enviarAuditoria(id, request);
    }
}