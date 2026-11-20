package fatec.fusex.nexus.auditoria;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorFatura.PdfGuiaFatura;
import fatec.fusex.nexus.setorFatura.PdfGuiaFaturaService;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@RestController
@RequestMapping("/auditoria/guias")
public class AuditoriaGuiaPdfController {

    private final PdfGuiaFaturaService pdfGuiaFaturaService;

    public AuditoriaGuiaPdfController(PdfGuiaFaturaService pdfGuiaFaturaService) {
        this.pdfGuiaFaturaService = pdfGuiaFaturaService;
    }

    @GetMapping("/{guiaId}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable Long guiaId) {
        PdfGuiaFatura pdf = pdfGuiaFaturaService.pdfDaGuia(guiaId, PerfilUsuario.AUDITORIA);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.nomeArquivo()).build().toString())
                .body(pdf.arquivo());
    }
}