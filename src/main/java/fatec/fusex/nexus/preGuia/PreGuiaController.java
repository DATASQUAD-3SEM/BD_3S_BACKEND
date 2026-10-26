package fatec.fusex.nexus.preGuia;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.preGuia.dto.CriarPreGuiaRequest;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;

/**
 * POST /pre-guias (multipart/form-data): o beneficiario logado cria a pre-guia com a OCS e o encaminhamento medico.
 * Devolve 201 com o numero da pre-guia criada.
 */
@RestController
@RequestMapping("/pre-guias")
public class PreGuiaController {

    private final PreGuiaService preGuiaService;

    public PreGuiaController(PreGuiaService preGuiaService) {
        this.preGuiaService = preGuiaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PreGuiaResponse> criar(@ModelAttribute CriarPreGuiaRequest request) {
        PreGuiaResponse resposta = preGuiaService.criar(request.getOcsId(), request.getArquivo());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
