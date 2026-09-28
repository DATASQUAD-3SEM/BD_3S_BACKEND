package fatec.fusex.nexus.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

/** Teste sem Spring (rapido): @TempDir cria uma pasta temporaria que e apagada no final. */
class ArmazenamentoLocalTest {

    @TempDir
    Path pastaTemporaria;

    @Test
    void salvaPdfEDevolveCaminhoRelativo() {
        ArmazenamentoLocal storage = new ArmazenamentoLocal(pastaTemporaria.toString());
        MockMultipartFile pdf = new MockMultipartFile("arquivo", "receita.pdf", "application/pdf", "conteudo".getBytes());

        String caminho = storage.salvar(pdf, "encaminhamentos");

        assertThat(caminho).startsWith("encaminhamentos/").endsWith(".pdf");
        assertThat(Files.exists(pastaTemporaria.resolve(caminho))).isTrue();
    }

    @Test
    void recusaFormatoInvalido() {
        ArmazenamentoLocal storage = new ArmazenamentoLocal(pastaTemporaria.toString());
        MockMultipartFile exe = new MockMultipartFile("arquivo", "virus.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> storage.salvar(exe, "encaminhamentos"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void recusaArquivoVazio() {
        ArmazenamentoLocal storage = new ArmazenamentoLocal(pastaTemporaria.toString());
        MockMultipartFile vazio = new MockMultipartFile("arquivo", "a.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.salvar(vazio, "encaminhamentos"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void recusaSairDaPastaRaiz() {
        ArmazenamentoLocal storage = new ArmazenamentoLocal(pastaTemporaria.toString());
        MockMultipartFile pdf = new MockMultipartFile("arquivo", "a.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> storage.salvar(pdf, "../fora"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void removeArquivoSalvo() {
        ArmazenamentoLocal storage = new ArmazenamentoLocal(pastaTemporaria.toString());
        MockMultipartFile png = new MockMultipartFile("arquivo", "foto.PNG", "image/png", "x".getBytes());
        String caminho = storage.salvar(png, "encaminhamentos");

        storage.remover(caminho);

        assertThat(Files.exists(pastaTemporaria.resolve(caminho))).isFalse();
    }
}
