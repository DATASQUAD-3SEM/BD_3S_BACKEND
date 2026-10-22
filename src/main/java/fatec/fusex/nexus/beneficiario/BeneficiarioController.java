package fatec.fusex.nexus.beneficiario;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;

@RestController
@RequestMapping("/beneficiarios")
public class BeneficiarioController {

    private final BeneficiarioService beneficiarioService;

    public BeneficiarioController(BeneficiarioService beneficiarioService) {
        this.beneficiarioService = beneficiarioService;
    }

    @GetMapping("/me")
    public BeneficiarioResponse me() {
        return beneficiarioService.buscarLogado();
    }
}