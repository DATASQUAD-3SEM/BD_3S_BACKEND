package fatec.fusex.nexus.guiaAssinatura;

import java.util.List;

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

import fatec.fusex.nexus.guiaAssinatura.dto.GuiaParaAssinarResponse;
import fatec.fusex.nexus.guiaAssinatura.dto.PdfGuiaAssinada;

@RestController
@RequestMapping("/guias")
public class GuiaAssinaturaController {

    private final GuiaAssinaturaService guiaAssinaturaService;
    private final GuiaPdfAssinadoService guiaPdfAssinadoService;

    public GuiaAssinaturaController(GuiaAssinaturaService guiaAssinaturaService,
                                    GuiaPdfAssinadoService guiaPdfAssinadoService) {
        this.guiaAssinaturaService = guiaAssinaturaService;
        this.guiaPdfAssinadoService = guiaPdfAssinadoService;
    }

    @GetMapping("/para-assinar")
    public List<GuiaParaAssinarResponse> listarParaAssinar() {
        return guiaAssinaturaService.listarParaAssinar();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable("id") Long id,
                                        @RequestParam(value = "modo", required = false) String modo) {
        PdfGuiaAssinada pdf = guiaPdfAssinadoService.pdfDaGuia(id, modo);

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