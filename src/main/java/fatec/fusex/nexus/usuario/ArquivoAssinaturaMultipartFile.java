package fatec.fusex.nexus.usuario;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.web.multipart.MultipartFile;

/**
 * Adapta os bytes de uma assinatura ja normalizada para a interface MultipartFile,
 * permitindo grava-los pelo mesmo {@code ArmazenamentoArquivo} usado nos uploads.
 */
final class ArquivoAssinaturaMultipartFile implements MultipartFile {

    private final byte[] conteudo;
    private final String nome;
    private final String contentType;

    ArquivoAssinaturaMultipartFile(
            byte[] conteudo, String nome, String contentType) {
        this.conteudo = conteudo.clone();
        this.nome = nome;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return "arquivo";
    }

    @Override
    public String getOriginalFilename() {
        return nome;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return conteudo.length == 0;
    }

    @Override
    public long getSize() {
        return conteudo.length;
    }

    @Override
    public byte[] getBytes() {
        return conteudo.clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(conteudo);
    }

    @Override
    public void transferTo(File destino) throws IOException {
        Files.copy(
                new ByteArrayInputStream(conteudo),
                destino.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }
}