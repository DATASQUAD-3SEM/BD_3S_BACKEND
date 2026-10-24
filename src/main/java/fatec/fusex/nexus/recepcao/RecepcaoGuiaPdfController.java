package fatec.fusex.nexus.recepcao;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recepcao/guias")
public class RecepcaoGuiaPdfController {

    private final RecepcaoGuiaPdfService recepcaoGuiaPdfService;

    public RecepcaoGuiaPdfController(RecepcaoGuiaPdfService recepcaoGuiaPdfService) {
        this.recepcaoGuiaPdfService = recepcaoGuiaPdfService;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable("id") Long id,
                                        @RequestParam(value = "modo", required = false) String modo) {
        PdfGuiaRecepcao pdf = recepcaoGuiaPdfService.pdfDaGuia(id, modo);

        ContentDisposition disposicao = ContentDisposition
                .builder(pdf.anexo() ? "attachment" : "inline")
                .filename(pdf.nomeArquivo())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .body(pdf.arquivo());
    }
}