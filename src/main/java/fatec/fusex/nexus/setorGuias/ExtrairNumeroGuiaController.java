package fatec.fusex.nexus.setorGuias;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.setorGuias.dto.ExtracaoNumeroGuiaResponse;

/**
 * POST /setor-guias/extrair-numero-guia (T11-BE1): le o numero da Guia no PDF enviado (multipart com o campo pdf).
 * Nao grava nada: o arquivo fica em memoria ate o "Concluir".
 */
@RestController
@RequestMapping("/setor-guias")
public class ExtrairNumeroGuiaController {

    private final ExtrairNumeroGuiaService extrairNumeroGuiaService;

    public ExtrairNumeroGuiaController(ExtrairNumeroGuiaService extrairNumeroGuiaService) {
        this.extrairNumeroGuiaService = extrairNumeroGuiaService;
    }

    // required = false: campo ausente cai na validacao do service (400)
    @PostMapping(value = "/extrair-numero-guia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtracaoNumeroGuiaResponse extrair(@RequestPart(required = false) MultipartFile pdf) {
        return extrairNumeroGuiaService.extrair(pdf);
    }
}
