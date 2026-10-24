package fatec.fusex.nexus.preGuia;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.preGuia.dto.CorrecaoPreGuiaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/beneficiarios/me/pre-guias")
public class BeneficiarioPreGuiaController {

    private final PreGuiaService preGuiaService;
    private final SimulacaoLogin simulacaoLogin;

    public BeneficiarioPreGuiaController(PreGuiaService preGuiaService, SimulacaoLogin simulacaoLogin) {
        this.preGuiaService = preGuiaService;
        this.simulacaoLogin = simulacaoLogin;
    }

    @GetMapping("/{id}/correcao")
    public CorrecaoPreGuiaResponse buscarCorrecao(@PathVariable Long id) {
        // Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return preGuiaService.buscarDadosCorrecao(id, simulacaoLogin.loginAtual());
    }
}
