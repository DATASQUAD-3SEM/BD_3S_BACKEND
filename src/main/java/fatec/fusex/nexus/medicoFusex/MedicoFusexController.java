package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.security.ContextoAutenticacao;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medicos")
public class MedicoFusexController {

    private final MedicoFusexService service;

    public MedicoFusexController(MedicoFusexService service) {
        this.service = service;
    }

    // T2-BE2: GET /medicos/me - dados do medico logado
    @GetMapping("/me")
    public MedicoMeResponse me(HttpServletRequest requisicao) {
        ContextoAutenticacao.exigirPerfil(requisicao, PerfilUsuario.MEDICO);
        return service.buscarLogado(ContextoAutenticacao.loginDe(requisicao));

    }
}