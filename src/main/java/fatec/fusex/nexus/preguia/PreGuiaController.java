package fatec.fusex.nexus.preguia;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.preguia.dto.CriarPreGuiaRequest;
import fatec.fusex.nexus.preguia.dto.PreGuiaResponse;

import jakarta.validation.Valid;

/**
 * SCRUM-30: POST /pre-guias multipart/form-data.
 */
@RestController
@RequestMapping("/pre-guias")
public class PreGuiaController {

    private final PreGuiaService preGuiaService;

    public PreGuiaController(PreGuiaService preGuiaService) {
        this.preGuiaService = preGuiaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PreGuiaResponse> criar(@Valid @ModelAttribute CriarPreGuiaRequest request) {
        PreGuiaResponse resposta = preGuiaService.criar(
                request.getCpf(),
                request.getPrecCp(),
                request.getOcsId(),
                request.getArquivo());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}