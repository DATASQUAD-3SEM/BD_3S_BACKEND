package fatec.fusex.nexus.guiaBeneficiario;

import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * T18-BE1: GET /guias/minhas, lista das guias do beneficiario logado.
 * T18-BE-2: POST /guias/{id}/envio, envia a guia pronta para a OCS da pre-guia
*/
@RestController
@RequestMapping("/guias")
public class GuiaBeneficiarioController {

    private final GuiaBeneficiarioService service;

    public GuiaBeneficiarioController(GuiaBeneficiarioService service) {
        this.service = service;
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<GuiaBeneficiarioResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping("/{id}/envio")
    public ResponseEntity<GuiaBeneficiarioResponse> enviar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.enviar(id));
    }
}
