package fatec.fusex.nexus.ocs;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.ocs.dto.OcsBuscaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocs")
public class OcsController {

    private final OcsService ocsService;
    private final SimulacaoLogin simulacaoLogin;
    private final MedicoFusexService medicoService;

    public OcsController(OcsService ocsService, SimulacaoLogin simulacaoLogin, MedicoFusexService medicoService) {
        this.ocsService = ocsService;
        this.simulacaoLogin = simulacaoLogin;
        this.medicoService = medicoService;
    }

    @GetMapping
    public OcsBuscaResponse buscar(@RequestParam(required = false) String busca) {
        String login = simulacaoLogin.loginAtual();
        medicoService.validarPerfilMedico(login); // 403 se não for médico ativo
        return ocsService.buscar(busca);
    }
}