package fatec.fusex.nexus.ocs;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.ocs.dto.OcsResponse;

@RestController
@RequestMapping("/beneficiarios/me/ocs")
public class OcsBeneficiarioController {

    private final OcsBeneficiarioService ocsBeneficiarioService;

    public OcsBeneficiarioController(OcsBeneficiarioService ocsBeneficiarioService) {
        this.ocsBeneficiarioService = ocsBeneficiarioService;
    }

    @GetMapping
    public List<OcsResponse> buscar(@RequestParam(required = false) String busca) {
        return ocsBeneficiarioService.buscarAtivas(busca);
    }
}