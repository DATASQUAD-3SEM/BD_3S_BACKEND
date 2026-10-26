package fatec.fusex.nexus.chefeBeneficiario;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeBeneficiario.dto.AlterarSituacaoBeneficiarioRequest;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioSituacaoResponse;

//T7-BE4: PATCH /chefe/beneficiarios/{id}/situacao
/**
 * PATCH /chefe/beneficiarios/{id}/situacao (T7-BE4): desativa ou reativa o beneficiario.
 * Nunca ha DELETE, para preservar o historico de pre-guias, Guias e assinaturas.
 */
@RestController
@RequestMapping("/chefe/beneficiarios")
public class ChefeBeneficiarioSituacaoController {

    private final ChefeBeneficiarioSituacaoService situacaoService;

    public ChefeBeneficiarioSituacaoController(ChefeBeneficiarioSituacaoService situacaoService) {
        this.situacaoService = situacaoService;
    }

    @PatchMapping("/{id}/situacao")
    public BeneficiarioSituacaoResponse alterarSituacao(
            @PathVariable Long id,
            @RequestBody(required = false) AlterarSituacaoBeneficiarioRequest request) {
        return situacaoService.alterar(id, request);
    }
}
