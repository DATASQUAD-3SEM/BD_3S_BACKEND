package fatec.fusex.nexus.setorGuias;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.setorGuias.dto.DetalhePreGuiaResponse;
import fatec.fusex.nexus.setorGuias.dto.FilaPreGuiaPaginaResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import fatec.fusex.nexus.minhasPreGuias.dto.EncaminhamentoArquivo;

/**
 * Endpoints de leitura do Setor de Guias: fila (T3-BE1), detalhe da pre-guia (T4-BE1)
 * e arquivo do encaminhamento (T4-BE2). Nenhum deles altera dados.
 */
@RestController
@RequestMapping("/setor-guias")
public class SetorGuiasController {

    private final SetorGuiasService setorGuiasService;

    public SetorGuiasController(SetorGuiasService setorGuiasService) {
        this.setorGuiasService = setorGuiasService;
    }

    @GetMapping("/pre-guias")
    public FilaPreGuiaPaginaResponse listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return setorGuiasService.listar(busca, status, page, size);
    }

    @GetMapping("/pre-guias/{id}")
    public DetalhePreGuiaResponse detalhar(@PathVariable Long id) {
        return setorGuiasService.detalhar(id);
    }

    @GetMapping("/pre-guias/{id}/encaminhamento")
    public ResponseEntity<Resource> baixarEncaminhamento(@PathVariable Long id) {
        EncaminhamentoArquivo arquivo = setorGuiasService.baixarEncaminhamento(id);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(arquivo.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(arquivo.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(arquivo.recurso());
    }
}