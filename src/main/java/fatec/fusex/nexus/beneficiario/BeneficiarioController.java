package fatec.fusex.nexus.beneficiario;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import fatec.fusex.nexus.beneficiario.dto.AtualizarTelefoneRequest;
import fatec.fusex.nexus.beneficiario.dto.TelefoneAtualizadoResponse;

@RestController
@RequestMapping("/beneficiarios")
public class BeneficiarioController {

    private final BeneficiarioService beneficiarioService;

    public BeneficiarioController(BeneficiarioService beneficiarioService) {
        this.beneficiarioService = beneficiarioService;
    }

    @GetMapping("/me")
    public BeneficiarioResponse me() {
        return beneficiarioService.buscarLogado();
    }

    @PatchMapping("/me/telefone")
    public TelefoneAtualizadoResponse atualizarTelefone(
            @RequestBody(required = false) AtualizarTelefoneRequest request) {

        String telefone = beneficiarioService.atualizarTelefoneLogado(
                request == null ? null : request.telefone()
        );

        return new TelefoneAtualizadoResponse(telefone);
    }
}