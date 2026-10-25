package fatec.fusex.nexus.common.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;

/**
 * Valida o arquivo de encaminhamento medico enviado pelo beneficiario, na criacao (POST /pre-guias)
 * e no reenvio da pre-guia (T9-BE4).
 *
 * Aceita somente PDF, JPG ou PNG. A extensao do nome e conferida junto com os primeiros bytes do arquivo,
 * para que um arquivo renomeado (ex.: um .exe chamado foto.jpg) seja recusado. Erros geram 422 no campo "arquivo".
 */
@Component
public class ValidacaoEncaminhamento {

    private static final byte[] ASSINATURA_PDF = "%PDF".getBytes();
    private static final byte[] ASSINATURA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final long tamanhoMaximoBytes;

    public ValidacaoEncaminhamento(
            @Value("${nexus.encaminhamento.tamanho-maximo-bytes:10485760}") long tamanhoMaximoBytes) {
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }

    /** Exige que o arquivo tenha sido enviado e valida formato e tamanho. */
    public void validarObrigatorio(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new DadosInvalidosException("arquivo", "Anexe o encaminhamento medico (PDF, JPG ou PNG)");
        }
        validar(arquivo);
    }

    /** Valida formato (PDF, JPG ou PNG) e tamanho de um arquivo ja enviado. */
    public void validar(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        String extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1) : "";
        byte[] esperado = switch (extensao) {
            case "pdf" -> ASSINATURA_PDF;
            case "jpg", "jpeg" -> ASSINATURA_JPG;
            case "png" -> ASSINATURA_PNG;
            default -> null;
        };
        if (esperado == null || !comecaCom(arquivo, esperado)) {
            throw new DadosInvalidosException("arquivo", "Formato invalido. Envie PDF, JPG ou PNG");
        }
        if (arquivo.getSize() > tamanhoMaximoBytes) {
            throw new DadosInvalidosException("arquivo",
                    "O arquivo deve ter no maximo " + (tamanhoMaximoBytes / (1024 * 1024)) + " MB");
        }
    }

    private static boolean comecaCom(MultipartFile arquivo, byte[] esperado) {
        try (InputStream entrada = arquivo.getInputStream()) {
            return Arrays.equals(entrada.readNBytes(esperado.length), esperado);
        } catch (IOException e) {
            return false;
        }
    }
}
