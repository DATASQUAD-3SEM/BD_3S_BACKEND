package fatec.fusex.nexus.beneficiario;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/beneficiarios")
public class BeneficiarioController {

    private final BeneficiarioService beneficiarioService;

    public BeneficiarioController(BeneficiarioService beneficiarioService) {
        this.beneficiarioService = beneficiarioService;
    }

    @GetMapping("/me")
    public BeneficiarioResponse me() {
        return BeneficiarioResponse.de(beneficiarioService.buscarPorCpf("52998224725"));
    }

}
