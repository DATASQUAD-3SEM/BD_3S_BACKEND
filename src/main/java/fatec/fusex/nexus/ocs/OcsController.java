package fatec.fusex.nexus.ocs;

import fatec.fusex.nexus.common.security.ContextoAutenticacao;
import fatec.fusex.nexus.ocs.dto.OcsBuscaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocs")
public class OcsController {

    private final OcsService ocsService;

    public OcsController(OcsService ocsService) {
        this.ocsService = ocsService;
    }

    @GetMapping
    public OcsBuscaResponse buscar(
            @RequestParam(required = false) String busca,
            HttpServletRequest requisicao) {

        ContextoAutenticacao.exigirPerfil(requisicao, PerfilUsuario.MEDICO);

        return ocsService.buscar(busca);

    }
}