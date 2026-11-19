package db.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T16-BE3, V13: a migration relê o PDF das Guias sem validade, grava a data de NAO RECEBER APOS e so torna a coluna
 * obrigatoria quando nao sobra nenhuma Guia sem validade. Roda contra H2 em modo MySQL, com o Flyway simulado.
 */
class V13GuiaValidadeObrigatoriaTest {

    // guia-modelo.pdf traz "NAO RECEBER APOS: 20/10/2026"
    private static final LocalDate VALIDADE_DO_MODELO = LocalDate.of(2026, 10, 20);

    @TempDir
    Path storage;

    private Connection conexao;

    @BeforeEach
    void preparar() throws Exception {
        conexao = DriverManager.getConnection(
                "jdbc:h2:mem:v13-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement sql = conexao.createStatement()) {
            sql.execute("CREATE TABLE Guia (id_guia BIGINT PRIMARY KEY, arquivo_url VARCHAR(500) NOT NULL, "
                    + "validade_ate DATE NULL)");
        }
        Files.createDirectories(storage.resolve("guias"));
    }

    @AfterEach
    void fechar() throws Exception {
        conexao.close();
    }

    @Test
    void guiasComDataNoPdfGanhamAValidadeESerraAColunaObrigatoria() throws Exception {
        copiarModelo("guias/a.pdf");
        inserir(1L, "guias/a.pdf", null);

        migrar();

        assertThat(validadeDe(1L)).isEqualTo(VALIDADE_DO_MODELO);
        assertThat(colunaAceitaNulo()).isFalse();
    }

    @Test
    void guiaSemDataNoPdfFicaSemValidadeELaColunaContinuaAnulavel() throws Exception {
        copiarModelo("guias/a.pdf");
        inserir(1L, "guias/a.pdf", null);
        inserir(2L, "guias/sem-data.pdf", null);
        pdfSemData("guias/sem-data.pdf");

        migrar();

        assertThat(validadeDe(1L)).isEqualTo(VALIDADE_DO_MODELO);
        assertThat(validadeDe(2L)).isNull();
        assertThat(colunaAceitaNulo()).isTrue();
    }

    @Test
    void arquivoAusenteFicaSemValidadeSemDerrubarAMigration() throws Exception {
        inserir(1L, "guias/nao-existe.pdf", null);

        migrar();

        assertThat(validadeDe(1L)).isNull();
        assertThat(colunaAceitaNulo()).isTrue();
    }

    @Test
    void guiaComValidadeJaGravadaNaoEAlterada() throws Exception {
        LocalDate gravada = LocalDate.of(2027, 1, 1);
        copiarModelo("guias/a.pdf");
        inserir(1L, "guias/a.pdf", gravada);

        migrar();

        assertThat(validadeDe(1L)).isEqualTo(gravada);
        assertThat(colunaAceitaNulo()).isFalse();
    }

    @Test
    void caminhoForaDaRaizEhIgnorado() throws Exception {
        inserir(1L, "../fora.pdf", null);

        migrar();

        assertThat(validadeDe(1L)).isNull();
        assertThat(colunaAceitaNulo()).isTrue();
    }

    @Test
    void depoisDeObrigatoriaUmaGuiaSemValidadeNaoEMaisAceita() throws Exception {
        copiarModelo("guias/a.pdf");
        inserir(1L, "guias/a.pdf", null);
        migrar();

        assertThatThrownBy(() -> inserir(2L, "guias/a.pdf", null)).isInstanceOf(Exception.class);
    }

    // ---------------------------------------------------------------- auxiliares

    private void migrar() throws Exception {
        Configuration configuracao = mock(Configuration.class);
        when(configuracao.getPlaceholders()).thenReturn(Map.of("storage-dir", storage.toString()));
        Context contexto = mock(Context.class);
        when(contexto.getConfiguration()).thenReturn(configuracao);
        when(contexto.getConnection()).thenReturn(conexao);

        new V13__guia_validade_obrigatoria().migrate(contexto);
    }

    private void inserir(long id, String arquivoUrl, LocalDate validade) throws Exception {
        try (var sql = conexao.prepareStatement("INSERT INTO Guia (id_guia, arquivo_url, validade_ate) VALUES (?, ?, ?)")) {
            sql.setLong(1, id);
            sql.setString(2, arquivoUrl);
            sql.setObject(3, validade);
            sql.executeUpdate();
        }
    }

    private LocalDate validadeDe(long id) throws Exception {
        try (var sql = conexao.prepareStatement("SELECT validade_ate FROM Guia WHERE id_guia = ?")) {
            sql.setLong(1, id);
            try (ResultSet resultado = sql.executeQuery()) {
                resultado.next();
                return resultado.getObject(1, LocalDate.class);
            }
        }
    }

    // Anulavel = aceita insert sem validade (teste direto: o H2 nao expoe a nulidade de forma uniforme)
    private boolean colunaAceitaNulo() throws Exception {
        try (Statement sql = conexao.createStatement()) {
            sql.execute("INSERT INTO Guia (id_guia, arquivo_url, validade_ate) VALUES (999999, 'teste.pdf', NULL)");
            sql.execute("DELETE FROM Guia WHERE id_guia = 999999");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void copiarModelo(String destino) throws Exception {
        try (InputStream modelo = getClass().getResourceAsStream("/guia-modelo.pdf")) {
            Files.write(storage.resolve(destino), modelo.readAllBytes());
        }
    }

    private void pdfSemData(String destino) throws Exception {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            documento.addPage(new PDPage());
            documento.save(saida);
            Files.write(storage.resolve(destino), saida.toByteArray());
        }
    }
}
