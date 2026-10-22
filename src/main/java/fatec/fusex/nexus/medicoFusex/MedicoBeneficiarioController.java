package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.beneficiario.BeneficiarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;

import jakarta.servlet.http.HttpServletRequest;


//T2-BE1: GET /medicos/beneficiarios?precCp={valor}
@RestController
@RequestMapping("/medicos/beneficiarios")
public class MedicoBeneficiarioController {

    private final BeneficiarioService beneficiarioService;

    public MedicoBeneficiarioController(BeneficiarioService beneficiarioService) {
        this.beneficiarioService = beneficiarioService;
    }

    @GetMapping
    public BeneficiarioMedicoResponse buscarPorPrecCp(
            // required = false: precCp ausente cai na validacao do service (400 no formato padrao de erro)
            @RequestParam(required = false) String precCp,
            HttpServletRequest requisicao) {

        // O perfil e checado ANTES de validar/buscar: quem nao e medico nao descobre se o Prec CP existe.
        if (!requisicao.isUserInRole(PerfilUsuario.MEDICO.name())) {
           throw new AcessoNegadoException("Acesso restrito a medicos");
        }
        return BeneficiarioMedicoResponse.de(beneficiarioService.buscarAtivoPorPrecCp(precCp));
    }
}
