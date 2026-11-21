package fatec.fusex.nexus.dependenteLeitura;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** T31-BE5 (SCRUM-274): leitura de dependentes para Minhas informacoes. */
@RestController
public class DependenteLeituraController {

    private final DependenteLeituraService service;
    private final ResponsavelMeService responsavelMeService;

    public DependenteLeituraController(DependenteLeituraService service, ResponsavelMeService responsavelMeService) {
        this.service = service;
        this.responsavelMeService = responsavelMeService;
    }

    /** Dados pessoais do responsavel legal externo (nome, CPF e se ja tem assinatura). */
    @GetMapping("/responsavel/me")
    public ResponsavelMeService.ResponsavelMeResponse meusDadosDeResponsavel() {
        return responsavelMeService.meusDados();
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
