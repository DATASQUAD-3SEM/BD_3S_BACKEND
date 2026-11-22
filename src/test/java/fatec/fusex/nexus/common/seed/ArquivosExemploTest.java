package fatec.fusex.nexus.common.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.guiaAssinatura.CarimboAssinaturaPdf;

/** T28-BE2 (SCRUM-249): arquivos de exemplo do seed de faturas (PDF da Guia e imagem da assinatura). */
class ArquivosExemploTest {

    private static final List<ArquivosExemplo.Procedimento> HEMOGRAMA =
            List.of(new ArquivosExemplo.Procedimento("40304361", "Hemograma completo", 2));

    @Test
    void pdfDaGuiaTrazOsDadosLidosPeloSetorDeGuias() throws IOException {
        byte[] pdf = ArquivosExemplo.pdfGuia("900000101", LocalDate.of(2099, 12, 31), "Antonio Carlos Ferreira",
                "45872213300", HEMOGRAMA);

        String texto;
        try (var documento = Loader.loadPDF(pdf)) {
            texto = new PDFTextStripper().getText(documento);
        }
        assertThat(texto).contains("GUIA DE ENCAMINHAMENTO Nr: 900000101")
                .contains("NÃO RECEBER APÓS: 31/12/2099")
                .contains("UG: Cmdo Bda Inf Amv")
                .contains("Titular: (PACIENTE)")
                .contains("45872213300")
                .contains("Hemograma completo 40304361 2");
    }

    @Test
    void pdfDaGuiaAceitaAsAssinaturasDoMedicoDoChefeEDoBeneficiario() {
        byte[] pdf = ArquivosExemplo.pdfGuia("900000101", LocalDate.of(2099, 12, 31), "Antonio Carlos Ferreira",
                "45872213300", HEMOGRAMA);
        byte[] assinatura = ArquivosExemplo.pngAssinatura("Exemplo");

        byte[] montado = new CarimboAssinaturaPdf().aplicar(pdf,
                Map.of(AssinaturaGuiaPapel.MEDICO, assinatura, AssinaturaGuiaPapel.CHEFE, assinatura), assinatura);

        assertThat(montado).isNotEmpty();
    }

    @Test
    void imagemDaAssinaturaEPng() {
        byte[] png = ArquivosExemplo.pngAssinatura("Carlos Eduardo Lima");

        assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    @SuppressWarnings("unchecked")
    void runnerGravaSoOsArquivosQueFaltamDentroDoArmazenamento(@TempDir Path raiz) throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any()))
                .thenAnswer(chamada -> {
                    String sql = chamada.getArgument(0);
                    if (sql.contains("FROM Guia g")) {
                        return List.of(guia("900000101", "seed/faturas/900000101.pdf"),
                                guia("900000999", "../fora-da-raiz.pdf"));
                    }
                    return List.of(assinatura("seed/assinaturas/medico-exemplo.png"));
                });
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(), eq(1L)))
                .thenReturn((List) HEMOGRAMA);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(), anyLong()))
                .thenReturn((List) HEMOGRAMA);

        ArquivosExemploSeedRunner runner = new ArquivosExemploSeedRunner(jdbc, raiz.toString());
        runner.run(null);

        Path pdf = raiz.resolve("seed/faturas/900000101.pdf");
        Path png = raiz.resolve("seed/assinaturas/medico-exemplo.png");
        assertThat(pdf).exists();
        assertThat(png).exists();
        assertThat(raiz.getParent().resolve("fora-da-raiz.pdf")).doesNotExist();

        byte[] existente = "ja existia".getBytes();
        Files.write(pdf, existente);
        runner.run(null);
        assertThat(Files.readAllBytes(pdf)).isEqualTo(existente);
    }

    private static Object guia(String numero, String url) throws Exception {
        return construir("ArquivosExemploSeedRunner$GuiaExemplo",
                new Class<?>[] {long.class, String.class, String.class, LocalDate.class, String.class, String.class},
                1L, numero, url, LocalDate.of(2099, 12, 31), "Antonio Carlos Ferreira", "45872213300");
    }

    private static Object assinatura(String url) throws Exception {
        return construir("ArquivosExemploSeedRunner$AssinaturaExemplo",
                new Class<?>[] {String.class, String.class}, url, "Carlos Eduardo Lima");
    }

    private static Object construir(String classe, Class<?>[] tipos, Object... valores) throws Exception {
        var construtor = Class.forName("fatec.fusex.nexus.common.seed." + classe).getDeclaredConstructor(tipos);
        construtor.setAccessible(true);
        return construtor.newInstance(valores);
    }
}
