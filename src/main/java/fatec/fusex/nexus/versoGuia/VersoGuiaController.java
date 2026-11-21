package fatec.fusex.nexus.versoGuia;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
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

import fatec.fusex.nexus.versoGuia.VersoGuiaService.PdfVerso;
import fatec.fusex.nexus.versoGuia.dto.VersoFaturaDetalheResponse;
import fatec.fusex.nexus.versoGuia.dto.VersoFaturaResponse;

/** T32-BE1: versos das Guias glosadas para a rastreabilidade imprimir (regra 15). */
@RestController
@RequestMapping("/rastreabilidade/versos")
public class VersoGuiaController {

    private final VersoGuiaService service;

    public VersoGuiaController(VersoGuiaService service) {
        this.service = service;
    }

    @GetMapping
    public List<VersoFaturaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{faturaId}")
    public VersoFaturaDetalheResponse detalhar(@PathVariable Long faturaId) {
        return service.detalhar(faturaId);
    }

    @GetMapping("/{faturaId}/guias/{guiaId}/pdf")
    public ResponseEntity<Resource> pdfDaGuia(@PathVariable Long faturaId, @PathVariable Long guiaId) {
        return pdf(service.pdfDaGuia(faturaId, guiaId));
    }

    @GetMapping("/{faturaId}/pdf")
    public ResponseEntity<Resource> pdfDaFatura(@PathVariable Long faturaId) {
        return pdf(service.pdfDaFatura(faturaId));
    }

    private static ResponseEntity<Resource> pdf(PdfVerso pdf) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.nomeArquivo()).build().toString())
                .body(new ByteArrayResource(pdf.conteudo()));
    }
}
