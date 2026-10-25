package fatec.fusex.nexus.MinhasPreGuias;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.MinhasPreGuias.dto.ReenvioPreGuiaResponse;

//T9-BE4: POST /beneficiarios/me/pre-guias/{id}/reenvio (multipart com ocsId e/ou arquivo)
@RestController
@RequestMapping("/beneficiarios/me/pre-guias")
public class ReenvioPreGuiaController {

    private final ReenvioPreGuiaService reenvioService;

    public ReenvioPreGuiaController(ReenvioPreGuiaService reenvioService) {
        this.reenvioService = reenvioService;
    }

    // Os dois campos sao opcionais: a falta dos dois cai na validacao do service (422)
    @PostMapping(value = "/{id}/reenvio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReenvioPreGuiaResponse reenviar(@PathVariable Long id,
                                           @RequestParam(required = false) Long ocsId,
                                           @RequestPart(required = false) MultipartFile arquivo) {
        return reenvioService.reenviar(id, ocsId, arquivo);
    }
}
