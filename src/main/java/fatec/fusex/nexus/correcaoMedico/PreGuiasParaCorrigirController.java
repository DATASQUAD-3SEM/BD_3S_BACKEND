package fatec.fusex.nexus.correcaoMedico;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.correcaoMedico.dto.PreGuiaParaCorrigirResponse;

//T14-BE1: GET /medicos/me/pre-guias/para-corrigir
/**
 * GET /medicos/me/pre-guias/para-corrigir (T14-BE1): pre-guias do medico logado devolvidas em Atencao.
 */
@RestController
@RequestMapping("/medicos/me/pre-guias")
public class PreGuiasParaCorrigirController {

    private final PreGuiasParaCorrigirService service;
    private final SimulacaoLogin simulacaoLogin;

    public PreGuiasParaCorrigirController(PreGuiasParaCorrigirService service, SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
    }

    @GetMapping("/para-corrigir")
    public List<PreGuiaParaCorrigirResponse> listar() {
        //Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return service.listar(simulacaoLogin.loginAtual());
    }
}
