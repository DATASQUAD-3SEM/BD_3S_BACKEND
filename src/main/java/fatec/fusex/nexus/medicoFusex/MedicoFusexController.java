package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints da Tela 2 (Nova pre-guia do medico): GET /medicos/me (T2-BE2)
 * e GET /medicos/procedimentos (T2-BE3, busca no catalogo inteiro, sem filtro por OCS).
 */
@RestController
@RequestMapping("/medicos")
public class MedicoFusexController {

    private final MedicoFusexService service;
    private final ProcedimentoExameService procedimentoService;
    private final SimulacaoLogin simulacaoLogin;

    public MedicoFusexController(MedicoFusexService service,
                                 ProcedimentoExameService procedimentoService,
                                 SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.procedimentoService = procedimentoService;
        this.simulacaoLogin = simulacaoLogin;
    }

    @GetMapping("/me")
    public MedicoMeResponse me() {
        //Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return service.buscarLogado(simulacaoLogin.loginAtual());
    }

    @GetMapping("/procedimentos")
    public List<ProcedimentoMedicoResponse> buscarProcedimentos(@RequestParam(required = false) String busca) {
        String login = simulacaoLogin.loginAtual();
        service.validarPerfilMedico(login); // 403 antes do 400: quem nao e medico nao ve a validacao da busca
        return procedimentoService.buscar(busca);
    }
}
