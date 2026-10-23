package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/medicos/procedimentos")
public class MedicoFusexController {

    private final MedicoFusexService service;

    public MedicoFusexController(MedicoFusexService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProcedimentoMedicoResponse> buscarProcedimentos(
            @RequestParam(required = false) String busca,
            HttpServletRequest requisicao) {

        // Mantém o padrão de segurança da T2-BE1
        //Para Testar sem o login: Desabilite as linhas: 31, 32 e 33!
        if (!requisicao.isUserInRole(PerfilUsuario.MEDICO.name())) {
            throw new AcessoNegadoException("Acesso restrito a medicos");
        }

        return service.buscar(busca);
    }
}