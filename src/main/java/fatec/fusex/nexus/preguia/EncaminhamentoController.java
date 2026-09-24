package fatec.fusex.nexus.preguia;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/encaminhamentos")
public class EncaminhamentoController {

    private static final List<String> TIPOS_PERMITIDOS = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    // Tamanho máximo de 10 Megabytes em bytes (10 * 1024 * 1024)
    private static final long TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024;

    private final ArmazenamentoArquivo armazenamentoArquivo;

    public EncaminhamentoController(ArmazenamentoArquivo armazenamentoArquivo) {
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadEncaminhamento(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RegraDeNegocioException("O arquivo de encaminhamento não foi enviado.");
        }

        if (file.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RegraDeNegocioException("O arquivo excede o tamanho máximo permitido de 10MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new RegraDeNegocioException("Tipo de arquivo inválido. Apenas PDF, JPG e PNG são permitidos.");
        }

        String caminhoRelativo = armazenamentoArquivo.salvar(file, "encaminhamentos");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "encaminhamentoUrl", caminhoRelativo
        ));
    }
}