package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.security.ContextoAutenticacao;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medicos")
public class MedicoFusexController {

    private final MedicoFusexService service;
    private final SimulacaoLogin simulacaoLogin;

    public MedicoFusexController(MedicoFusexService service, SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
    }

    @GetMapping("/me")
    public MedicoMeResponse me() {
        // Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        return service.buscarLogado(simulacaoLogin.loginAtual());
    }
}