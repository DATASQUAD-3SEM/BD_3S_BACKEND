package fatec.fusex.nexus.correcaoMedico;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.correcaoMedico.dto.CorrecaoMedicoResponse;

//T14-BE2: GET /medicos/me/pre-guias/{id}/correcao
/**
 * GET /medicos/me/pre-guias/{id}/correcao (T14-BE2): dados da pre-guia que o medico vai corrigir.
 */
@RestController
@RequestMapping("/medicos/me/pre-guias")
public class CorrecaoMedicoController {

    private final CorrecaoMedicoService service;
    private final SimulacaoLogin simulacaoLogin;

    public CorrecaoMedicoController(CorrecaoMedicoService service, SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
    }

    @GetMapping("/{id}/correcao")
    public CorrecaoMedicoResponse buscar(@PathVariable Long id) {
        //Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return service.buscar(id, simulacaoLogin.loginAtual());
    }
}
