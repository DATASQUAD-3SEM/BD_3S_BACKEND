package fatec.fusex.nexus.correcaoMedico;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.correcaoMedico.dto.ProcedimentoCorrecaoResponse;

//T14-BE4: GET /medicos/me/procedimentos?busca={texto}
/**
 * GET /medicos/me/procedimentos (T14-BE4): busca no catalogo de procedimentos para a tela de correcao.
 */
@RestController
@RequestMapping("/medicos/me/procedimentos")
public class ProcedimentoCorrecaoController {

    private final ProcedimentoCorrecaoService service;
    private final SimulacaoLogin simulacaoLogin;

    public ProcedimentoCorrecaoController(ProcedimentoCorrecaoService service, SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
    }

    @GetMapping
    public List<ProcedimentoCorrecaoResponse> buscar(@RequestParam(required = false) String busca) {
        //Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return service.buscar(simulacaoLogin.loginAtual(), busca);
    }
}
