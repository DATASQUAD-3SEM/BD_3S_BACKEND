package fatec.fusex.nexus.setorGuias;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorGuias.dto.GuiaConcluidaResponse;

//T4-BE6: POST /setor-guias/pre-guias/{id}/guia/concluir, sem corpo
@RestController
@RequestMapping("/setor-guias/pre-guias")
public class ConcluirGuiaController {

    private final ConcluirGuiaService concluirGuiaService;

    public ConcluirGuiaController(ConcluirGuiaService concluirGuiaService) {
        this.concluirGuiaService = concluirGuiaService;
    }

    @PostMapping("/{id}/guia/concluir")
    public ResponseEntity<GuiaConcluidaResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(concluirGuiaService.concluir(id));
    }
}
