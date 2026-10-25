package fatec.fusex.nexus.chefeFusex;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.chefeFusex.dto.AlterarSituacaoMedicoRequest;
import fatec.fusex.nexus.chefeFusex.dto.MedicoSituacaoResponse;

//T8-BE4: PATCH /chefe/medicos/{id}/situacao
/**
 * PATCH /chefe/medicos/{id}/situacao (T8-BE4): desativa ou reativa o medico, sem DELETE.
 */
@RestController
@RequestMapping("/chefe/medicos")
public class ChefeMedicoSituacaoController {

    private final ChefeMedicoSituacaoService situacaoService;

    public ChefeMedicoSituacaoController(ChefeMedicoSituacaoService situacaoService) {
        this.situacaoService = situacaoService;
    }

    @PatchMapping("/{id}/situacao")
    public MedicoSituacaoResponse alterarSituacao(
            @PathVariable Long id,
            @RequestBody(required = false) AlterarSituacaoMedicoRequest request) {
        return situacaoService.alterar(id, request);
    }
}
