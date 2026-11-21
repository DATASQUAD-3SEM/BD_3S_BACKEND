package fatec.fusex.nexus.dependenteLeitura;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** T31-BE5 (SCRUM-274): leitura de dependentes para Minhas informacoes. */
@RestController
public class DependenteLeituraController {

    private final DependenteLeituraService service;

    public DependenteLeituraController(DependenteLeituraService service) {
        this.service = service;
    }

    @GetMapping("/beneficiario/dependentes")
    public List<DependenteLeituraService.DependenteDoTitularResponse> dependentesDoTitular() {
        return service.dependentesDoTitular();
    }

    @GetMapping("/responsavel/menores")
    public List<DependenteLeituraService.MenorAcompanhadoResponse> menoresDoResponsavel() {
        return service.menoresDoResponsavel();
    }

    @GetMapping("/dependente/me")
    public DependenteLeituraService.DependenteMeResponse meuCadastro() {
        return service.meuCadastro();
    }
}
