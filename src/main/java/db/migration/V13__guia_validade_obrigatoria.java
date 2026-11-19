package db.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fatec.fusex.nexus.guia.ValidadeObrigatoriaGuia;
import fatec.fusex.nexus.setorGuias.CorrecaoValidadeGuias;

/**
 * T16-BE3, V13. Antes de tornar Guia.validade_ate obrigatoria, relê o PDF guardado das Guias sem validade (a mesma
 * correcao do CorrecaoValidadeGuias) e so entao aplica NOT NULL, se nenhuma Guia ficou sem validade. Assim a
 * migration nao falha em banco com Guias antigas. Quando sobra Guia sem data no PDF, a coluna continua anulavel e o
 * CorrecaoValidadeGuiasRunner aplica o NOT NULL depois que a equipe corrigir as que sobraram.
 *
 * Caminho dos arquivos: placeholder "storage-dir" (spring.flyway.placeholders.storage-dir, igual a nexus.storage.local-dir).
 * Os nomes seguem o arquivo SQL anterior para o Flyway reconhecer a mesma descricao da migration.
 */
public class V13__guia_validade_obrigatoria extends BaseJavaMigration {

    static final String PLACEHOLDER_STORAGE = "storage-dir";

    private static final Logger LOG = LoggerFactory.getLogger(V13__guia_validade_obrigatoria.class);

    @Override
    public void migrate(Context context) throws Exception {
        Path raiz = Path.of(storageDir(context)).toAbsolutePath().normalize();
        Connection conexao = context.getConnection();

        Map<Long, String> semValidade = new LinkedHashMap<>();
        try (Statement sql = conexao.createStatement();
             ResultSet resultado = sql.executeQuery("SELECT id_guia, arquivo_url FROM Guia WHERE validade_ate IS NULL")) {
            while (resultado.next()) {
                semValidade.put(resultado.getLong(1), resultado.getString(2));
            }
        }

        for (Map.Entry<Long, String> guia : semValidade.entrySet()) {
            Optional<LocalDate> validade = lerValidade(raiz, guia.getValue(), guia.getKey());
            if (validade.isPresent()) {
                gravarValidade(conexao, guia.getKey(), validade.get());
            }
        }

        if (!ValidadeObrigatoriaGuia.aplicarSeNaoHouverNulos(conexao)) {
            LOG.warn("validade_ate continua anulavel: ainda ha Guia sem validade. Corrigir a mao e reiniciar a aplicacao.");
        }
    }

    private static String storageDir(Context context) {
        String diretorio = context.getConfiguration().getPlaceholders().get(PLACEHOLDER_STORAGE);
        if (diretorio == null || diretorio.isBlank()) {
            throw new IllegalStateException("Placeholder " + PLACEHOLDER_STORAGE + " nao configurado para o Flyway");
        }
        return diretorio;
    }

    // PDF ausente, fora da raiz ou ilegivel vira "sem data": nao derruba a migration
    private static Optional<LocalDate> lerValidade(Path raiz, String caminho, Long idGuia) {
        try {
            Path arquivo = raiz.resolve(caminho).normalize();
            if (!arquivo.startsWith(raiz) || !Files.isRegularFile(arquivo)) {
                LOG.warn("Arquivo da Guia nao encontrado (id={}, caminho={})", idGuia, caminho);
                return Optional.empty();
            }
            Optional<LocalDate> validade = CorrecaoValidadeGuias.validadeDoPdf(Files.readAllBytes(arquivo));
            if (validade.isEmpty()) {
                LOG.warn("Guia sem validade: o PDF nao traz NAO RECEBER APOS (id={})", idGuia);
            }
            return validade;
        } catch (Exception e) {
            LOG.warn("Nao foi possivel ler o PDF da Guia (id={}): {}", idGuia, e.getMessage());
            return Optional.empty();
        }
    }

    private static void gravarValidade(Connection conexao, Long idGuia, LocalDate validade) throws Exception {
        try (PreparedStatement sql = conexao.prepareStatement(
                "UPDATE Guia SET validade_ate = ? WHERE id_guia = ? AND validade_ate IS NULL")) {
            sql.setDate(1, java.sql.Date.valueOf(validade));
            sql.setLong(2, idGuia);
            sql.executeUpdate();
        }
    }
}
