package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.beneficiario.BeneficiarioService;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /medicos/beneficiarios?precCp= (T2-BE1): o medico busca o beneficiario pelo Prec CP durante a consulta.
 */
@RestController
@RequestMapping("/medicos/beneficiarios")
public class MedicoBeneficiarioController {

    private final BeneficiarioService beneficiarioService;
    private final SimulacaoLogin simulacaoLogin;
    private final MedicoFusexService medicoService;

    public MedicoBeneficiarioController(BeneficiarioService beneficiarioService,
                                        SimulacaoLogin simulacaoLogin,
                                        MedicoFusexService medicoService) {
        this.beneficiarioService = beneficiarioService;
        this.simulacaoLogin = simulacaoLogin;
        this.medicoService = medicoService;
    }

    @GetMapping
    public BeneficiarioMedicoResponse buscarPorPrecCp(@RequestParam(required = false) String precCp) {
        //Pega o login do Header
        String login = simulacaoLogin.loginAtual();
        //Valida se é médico ativo (lança 403 se não for)
        medicoService.validarAcessoMedico(login);
        //Retorna o beneficiário
        return BeneficiarioMedicoResponse.de(beneficiarioService.buscarAtivoPorPrecCp(precCp));
    }
}