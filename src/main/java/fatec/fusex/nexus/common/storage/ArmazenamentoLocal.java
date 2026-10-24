package fatec.fusex.nexus.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;


@Service
public class ArmazenamentoLocal implements ArmazenamentoArquivo {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("pdf", "jpg", "jpeg", "png");

    private final Path raiz;

    public ArmazenamentoLocal(@Value("${nexus.storage.local-dir}") String diretorio) {
        this.raiz = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public String salvar(MultipartFile arquivo, String pasta) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("Nenhum arquivo foi enviado");
        }
        String extensao = extensaoDe(arquivo.getOriginalFilename());
        if (!EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new RegraDeNegocioException("Formato invalido. Envie PDF, JPG ou PNG");
        }

        Path pastaDestino = resolverDentroDaRaiz(pasta);
        String nomeNovo = UUID.randomUUID() + "." + extensao;

        try (InputStream entrada = arquivo.getInputStream()) {
            Files.createDirectories(pastaDestino);
            Files.copy(entrada, pastaDestino.resolve(nomeNovo));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar o arquivo", e);
        }
        return pasta + "/" + nomeNovo;
    }

    @Override
    public Resource carregar(String caminhoRelativo) {
        Path arquivo = resolverDentroDaRaiz(caminhoRelativo);
        if (!Files.isRegularFile(arquivo)) {
            throw new RecursoNaoEncontradoException("Arquivo nao encontrado no armazenamento");
        }
        return new FileSystemResource(arquivo);
    }

    @Override
    public void remover(String caminhoRelativo) {
        Path arquivo = resolverDentroDaRaiz(caminhoRelativo);
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao remover o arquivo", e);
        }
    }

    private Path resolverDentroDaRaiz(String caminho) {
        Path resolvido = raiz.resolve(caminho).normalize();
        if (!resolvido.startsWith(raiz)) {
            throw new RegraDeNegocioException("Caminho invalido");
        }
        return resolvido;
    }

    private String extensaoDe(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            return "";
        }
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
