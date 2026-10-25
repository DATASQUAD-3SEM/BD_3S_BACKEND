package fatec.fusex.nexus.minhasPreGuias;

import fatec.fusex.nexus.minhasPreGuias.dto.CorrecaoPreGuiaResponse;
import fatec.fusex.nexus.minhasPreGuias.dto.EncaminhamentoArquivo;
import fatec.fusex.nexus.minhasPreGuias.dto.GuiaPdf;
import fatec.fusex.nexus.minhasPreGuias.dto.MinhaPreGuiaResponse;

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

import java.util.List;

@RestController
@RequestMapping("/beneficiarios/me/pre-guias")
public class MinhasPreGuiasController {

    private final MinhasPreGuiasService minhasPreGuiasService;

    public MinhasPreGuiasController(MinhasPreGuiasService minhasPreGuiasService) {
        this.minhasPreGuiasService = minhasPreGuiasService;
    }

    @GetMapping
    public ResponseEntity<List<MinhaPreGuiaResponse>> listar() {
        List<MinhaPreGuiaResponse> response = minhasPreGuiasService.listar();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/correcao")
    public ResponseEntity<CorrecaoPreGuiaResponse> buscarCorrecao(@PathVariable Long id) {
        return ResponseEntity.ok(minhasPreGuiasService.buscarCorrecao(id));
    }

    @GetMapping("/{id}/encaminhamento/arquivo")
    public ResponseEntity<Resource> baixarEncaminhamento(@PathVariable Long id) {
        EncaminhamentoArquivo arquivo = minhasPreGuiasService.baixarEncaminhamento(id);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(arquivo.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(arquivo.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(arquivo.recurso());
    }

    @GetMapping("/{id}/guia/pdf")
    public ResponseEntity<Resource> baixarGuiaPdf(
            @PathVariable Long id,
            @RequestParam(required = false) String modo) {

        GuiaPdf pdf = minhasPreGuiasService.baixarGuiaPdf(id, modo);

        ContentDisposition disposition = (pdf.inline()
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(pdf.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf.resource());
    }
}