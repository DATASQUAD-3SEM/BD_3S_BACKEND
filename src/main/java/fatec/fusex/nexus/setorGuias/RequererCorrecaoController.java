package fatec.fusex.nexus.setorGuias;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorGuias.dto.CorrecaoRequeridaResponse;
import fatec.fusex.nexus.setorGuias.dto.RequererCorrecaoRequest;

//T4-BE4: POST /setor-guias/pre-guias/{id}/correcao
/**
 * POST /setor-guias/pre-guias/{id}/correcao (T4-BE4): devolve a pre-guia em Atencao com uma observacao.
 */
@RestController
@RequestMapping("/setor-guias/pre-guias")
public class RequererCorrecaoController {

    private final RequererCorrecaoService correcaoService;

    public RequererCorrecaoController(RequererCorrecaoService correcaoService) {
        this.correcaoService = correcaoService;
    }

    @PostMapping("/{id}/correcao")
    public CorrecaoRequeridaResponse requerer(@PathVariable Long id,
                                              @RequestBody(required = false) RequererCorrecaoRequest request) {
        return correcaoService.requerer(id, request);
    }
}
