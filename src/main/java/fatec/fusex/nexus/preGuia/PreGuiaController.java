package fatec.fusex.nexus.preGuia;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.dependente.PacienteResponse;
import fatec.fusex.nexus.preGuia.dto.CriarPreGuiaRequest;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;

/**
 * POST /pre-guias (multipart/form-data): o beneficiario logado cria a pre-guia com a OCS e o encaminhamento medico.
 * Devolve 201 com o numero da pre-guia criada.
 */
@RestController
@RequestMapping("/pre-guias")
public class PreGuiaController {

    private final PreGuiaService preGuiaService;

    public PreGuiaController(PreGuiaService preGuiaService) {
        this.preGuiaService = preGuiaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PreGuiaResponse> criar(@ModelAttribute CriarPreGuiaRequest request) {
        PreGuiaResponse resposta = preGuiaService.criar(request.getOcsId(), request.getDependenteId(), request.getArquivo());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    // T30-BE2: pacientes que o usuario pode escolher para a nova pre-guia
    @GetMapping("/pacientes")
    public List<PacienteResponse> pacientes() {
        return preGuiaService.listarPacientes();
    }
}
