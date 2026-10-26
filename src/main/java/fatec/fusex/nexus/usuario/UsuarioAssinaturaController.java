package fatec.fusex.nexus.usuario;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.usuario.dto.AssinaturaAtualizadaResponse;
import fatec.fusex.nexus.usuario.dto.AssinaturaBase64Request;

/**
 * PUT /usuarios/me/assinatura (T13-BE2): cadastra ou substitui a assinatura digital do usuario logado,
 * recebendo um arquivo (multipart) ou uma imagem em base64 desenhada no quadro.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioAssinaturaController {

    private final UsuarioAssinaturaService assinaturaService;

    public UsuarioAssinaturaController(
            UsuarioAssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @PutMapping(
            value = "/me/assinatura",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssinaturaAtualizadaResponse atualizarArquivo(
            @RequestPart(value = "arquivo", required = false)
            MultipartFile arquivo) {

        assinaturaService.atualizarComArquivo(arquivo);

        return new AssinaturaAtualizadaResponse(true);
    }

    @PutMapping(
            value = "/me/assinatura",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssinaturaAtualizadaResponse atualizarBase64(
            @RequestBody(required = false)
            AssinaturaBase64Request request) {

        String imagem = request == null ? null : request.imagemBase64();

        assinaturaService.atualizarComBase64(imagem);

        return new AssinaturaAtualizadaResponse(true);
    }
}