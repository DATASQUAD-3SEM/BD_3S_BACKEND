package fatec.fusex.nexus.setorGuias;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.setorGuias.dto.GuiaRascunhoResponse;

//T4-BE5: PUT /setor-guias/pre-guias/{id}/guia-rascunho (multipart com numeroGuia, pdf e exames)
/**
 * PUT /setor-guias/pre-guias/{id}/guia-rascunho (T4-BE5, T22-BE1): anexa ou troca o numero, o PDF e a lista
 * de exames da Guia (multipart; exames em texto JSON).
 */
@RestController
@RequestMapping("/setor-guias/pre-guias")
public class AnexarGuiaController {

    private final AnexarGuiaService anexarGuiaService;

    public AnexarGuiaController(AnexarGuiaService anexarGuiaService) {
        this.anexarGuiaService = anexarGuiaService;
    }

    // required = false: campo ausente cai na validacao do service (422 apontando o campo)
    // T16-BE2: ug, ugOrigem e validadeAte sao obrigatorios (ausentes caem na validacao do service: 422)
    @PutMapping(value = "/{id}/guia-rascunho", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GuiaRascunhoResponse anexar(@PathVariable Long id,
                                       @RequestParam(required = false) String numeroGuia,
                                       @RequestPart(required = false) MultipartFile pdf,
                                       @RequestParam(required = false) String exames,
                                       @RequestParam(required = false) String ug,
                                       @RequestParam(required = false) String ugOrigem,
                                       @RequestParam(required = false) String validadeAte) {
        return anexarGuiaService.anexar(id, numeroGuia, pdf, exames, ug, ugOrigem, validadeAte);
    }
}
