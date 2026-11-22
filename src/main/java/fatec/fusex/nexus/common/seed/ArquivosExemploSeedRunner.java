package fatec.fusex.nexus.common.seed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * T28-BE2 (SCRUM-249): so no perfil dev, grava no armazenamento local os arquivos que o seed de faturas
 * (R__faturas_exemplo.sql) referencia e que o SQL nao consegue criar: o PDF de cada Guia de exemplo
 * (Guia.arquivo_url "seed/...") e a imagem de cada assinatura de exemplo (Assinatura.assinatura_url "seed/...").
 * Arquivo que ja existe nao e sobrescrito. Com isso, "Ver guia (PDF)" funciona com os dados do seed.
 */
@Component
@Profile("dev")
public class ArquivosExemploSeedRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ArquivosExemploSeedRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final Path raiz;

    public ArquivosExemploSeedRunner(JdbcTemplate jdbcTemplate,
                                     @Value("${nexus.storage.local-dir}") String diretorio) {
        this.jdbcTemplate = jdbcTemplate;
        this.raiz = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) {
        int criados = 0;
        for (GuiaExemplo guia : guiasDoSeed()) {
            criados += gravarSeFaltar(guia.arquivoUrl(), () -> ArquivosExemplo.pdfGuia(guia.numero(),
                    guia.validadeAte(), guia.paciente(), guia.precCp(), procedimentos(guia.idGuia())));
        }
        for (AssinaturaExemplo assinatura : assinaturasDoSeed()) {
            criados += gravarSeFaltar(assinatura.url(), () -> ArquivosExemplo.pngAssinatura(assinatura.nome()));
        }
        if (criados > 0) {
            LOG.info("Seed de faturas: {} arquivo(s) de exemplo gravado(s) em {}", criados, raiz);
        }
    }

    private List<GuiaExemplo> guiasDoSeed() {
        return jdbcTemplate.query("""
                SELECT g.id_guia, g.numero, g.arquivo_url, g.validade_ate, b.nome, b.prec_cp
                FROM Guia g
                JOIN PreGuia p ON p.id_pre_guia = g.pre_guia_id
                JOIN Beneficiario b ON b.usuario_id = p.beneficiario_id
                WHERE g.arquivo_url LIKE 'seed/%'
                ORDER BY g.numero
                """, (rs, i) -> new GuiaExemplo(rs.getLong("id_guia"), rs.getString("numero"),
                rs.getString("arquivo_url"), rs.getObject("validade_ate", LocalDate.class), rs.getString("nome"),
                rs.getString("prec_cp")));
    }

    private List<ArquivosExemplo.Procedimento> procedimentos(long idGuia) {
        return jdbcTemplate.query("""
                SELECT cod_dgp, descricao_procedimento, quantidade_autorizada
                FROM GuiaProcedimento WHERE guia_id = ? ORDER BY id_guia_procedimento
                """, (rs, i) -> new ArquivosExemplo.Procedimento(rs.getString("cod_dgp"),
                rs.getString("descricao_procedimento"), rs.getInt("quantidade_autorizada")), idGuia);
    }

    private List<AssinaturaExemplo> assinaturasDoSeed() {
        return jdbcTemplate.query("""
                SELECT a.assinatura_url,
                       COALESCE(b.nome, m.nome, c.nome, u.login) AS nome
                FROM Assinatura a
                JOIN Usuario u ON u.id_usuario = a.usuario_id
                LEFT JOIN Beneficiario b ON b.usuario_id = u.id_usuario
                LEFT JOIN MedicoFusex m ON m.usuario_id = u.id_usuario
                LEFT JOIN ChefeFusex c ON c.usuario_id = u.id_usuario
                WHERE a.assinatura_url LIKE 'seed/%'
                """, (rs, i) -> new AssinaturaExemplo(rs.getString("assinatura_url"), rs.getString("nome")));
    }

    /** Grava o arquivo no caminho relativo (dentro da raiz do armazenamento) se ele ainda nao existir. */
    private int gravarSeFaltar(String caminhoRelativo, java.util.function.Supplier<byte[]> conteudo) {
        Path destino = raiz.resolve(caminhoRelativo).normalize();
        if (!destino.startsWith(raiz) || Files.exists(destino)) {
            return 0;
        }
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo.get());
            return 1;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o arquivo de exemplo " + caminhoRelativo, e);
        }
    }

    private record GuiaExemplo(long idGuia, String numero, String arquivoUrl, LocalDate validadeAte, String paciente,
                               String precCp) {
    }

    private record AssinaturaExemplo(String url, String nome) {
    }
}
