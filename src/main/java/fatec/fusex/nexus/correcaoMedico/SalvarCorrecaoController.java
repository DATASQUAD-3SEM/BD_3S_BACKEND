package fatec.fusex.nexus.correcaoMedico;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.correcaoMedico.dto.CorrecaoReenviadaResponse;
import fatec.fusex.nexus.correcaoMedico.dto.SalvarCorrecaoRequest;

//T14-BE5: PUT /medicos/me/pre-guias/{id}/correcao (o GET do mesmo caminho fica no CorrecaoMedicoController, T14-BE2)
@RestController
@RequestMapping("/medicos/me/pre-guias")
public class SalvarCorrecaoController {

    private final SalvarCorrecaoService service;
    private final SimulacaoLogin simulacaoLogin;

    public SalvarCorrecaoController(SalvarCorrecaoService service, SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
    }

    @PutMapping("/{id}/correcao")
    public CorrecaoReenviadaResponse salvar(@PathVariable Long id,
                                            @RequestBody(required = false) SalvarCorrecaoRequest request) {
        //Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return service.salvar(id, simulacaoLogin.loginAtual(), request);
    }
}
