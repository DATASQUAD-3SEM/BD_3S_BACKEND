package fatec.fusex.nexus.correcaoMedico;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.correcaoMedico.dto.OcsCorrecaoResponse;

//T14-BE3: GET /medicos/me/ocs?busca={texto}
/**
 * GET /medicos/me/ocs (T14-BE3): busca de OCS na tela de correcao do medico.
 * E um endpoint proprio, separado do GET /ocs (T2-BE2), para manter as tasks independentes.
 */
@RestController
@RequestMapping("/medicos/me/ocs")
public class OcsCorrecaoController {

    private final OcsCorrecaoService service;
    private final SimulacaoLogin simulacaoLogin;

    public OcsCorrecaoController(OcsCorrecaoService service, SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
    }

    @GetMapping
    public OcsCorrecaoResponse buscar(@RequestParam(required = false) String busca) {
        //Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return service.buscar(simulacaoLogin.loginAtual(), busca);
    }
}
