package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.preGuia.MedicoPreGuiaService;
import fatec.fusex.nexus.preGuia.dto.CriarPreGuiaMedicoRequest;
import fatec.fusex.nexus.preGuia.dto.PreGuiaMedicoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//T2-BE4: POST /medicos/pre-guias
/**
 * POST /medicos/pre-guias (T2-BE4): o medico cria e envia a pre-guia durante a consulta. Devolve 201.
 */
@RestController
@RequestMapping("/medicos/pre-guias")
public class MedicoPreGuiaController {

    private final MedicoPreGuiaService medicoPreGuiaService;
    private final SimulacaoLogin simulacaoLogin;

    public MedicoPreGuiaController(MedicoPreGuiaService medicoPreGuiaService, SimulacaoLogin simulacaoLogin) {
        this.medicoPreGuiaService = medicoPreGuiaService;
        this.simulacaoLogin = simulacaoLogin;
    }

    @PostMapping
    public ResponseEntity<PreGuiaMedicoResponse> criar(@RequestBody CriarPreGuiaMedicoRequest request) {
        //Pega o login do header X-Usuario-Login (se não tiver, lança 401)
        String login = simulacaoLogin.loginAtual();
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoPreGuiaService.criar(login, request));
    }
}
