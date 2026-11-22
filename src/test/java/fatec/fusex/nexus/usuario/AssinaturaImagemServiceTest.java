package fatec.fusex.nexus.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;

/**
 * Testes da validacao e normalizacao da imagem da assinatura (T13-BE2): formatos, imagem em branco e tamanho.
 */
class AssinaturaImagemServiceTest {

    private final AssinaturaImagemService service =
            new AssinaturaImagemService(2 * 1024 * 1024L, 1200, 600);

    @Test
    void normalizarUpload_quandoAssinaturaPng_geraFundoTransparenteComTracoOpaco()
            throws IOException {

        byte[] original = criarAssinaturaPng(640, 180);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "assinatura.png", "image/png", original);

        byte[] resultado = service.normalizarUpload(arquivo);

        BufferedImage imagem = ImageIO.read(
                new ByteArrayInputStream(resultado));

        assertThat(imagem).isNotNull();
        // T37-BE1: toda assinatura guardada tem 900 x 300
        assertThat(imagem.getWidth()).isEqualTo(900);
        assertThat(imagem.getHeight()).isEqualTo(300);

        Color canto = new Color(imagem.getRGB(0, 0), true);
        assertThat(canto.getAlpha()).isEqualTo(0);

        assertThat(contarPixelsOpacos(imagem)).isGreaterThan(0);
    }

    @Test
    void normalizarUpload_quandoImagemJpg_aceitaEConverteParaPng()
            throws IOException {

        byte[] original = criarJpeg(300, 100);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "assinatura.jpg", "image/jpeg", original);

        byte[] resultado = service.normalizarUpload(arquivo);

        BufferedImage imagem = ImageIO.read(
                new ByteArrayInputStream(resultado));

        assertThat(imagem).isNotNull();
        assertThat(imagem.getWidth()).isEqualTo(900);
        assertThat(resultado.length).isLessThanOrEqualTo(2 * 1024 * 1024);
        assertThat(new Color(imagem.getRGB(0, 0), true).getAlpha()).isEqualTo(0);
        assertThat(contarPixelsOpacos(imagem)).isGreaterThan(0);
    }

    @Test
    void normalizarUpload_quandoImagemExcedeDimensoesDeLeitura_leReduzidaESaiNoTamanhoFixo()
            throws IOException {

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                "image/png",
                criarAssinaturaPng(2400, 1200));

        AssinaturaImagemService servicoComLimitesMenores =
                new AssinaturaImagemService(
                        2 * 1024 * 1024L, 120, 60);

        byte[] resultado =
                servicoComLimitesMenores.normalizarUpload(arquivo);

        BufferedImage imagem = ImageIO.read(
                new ByteArrayInputStream(resultado));

        assertThat(imagem.getWidth()).isEqualTo(900);
        assertThat(imagem.getHeight()).isEqualTo(300);
    }

    @Test
    void normalizar_tracoPequenoNumCantoEGrandeNoCentro_saemComAMesmaLarguraUtil() throws IOException {
        // Mesmo traco (proporcao 4:1) desenhado pequeno no canto de um quadro grande e grande no centro.
        byte[] pequeno = criarTraco(1000, 500, 20, 20, 120, 30);
        byte[] grande = criarTraco(1000, 500, 100, 150, 800, 200);

        BufferedImage a = lerPng(service.normalizarUpload(new MockMultipartFile("arquivo", "a.png", "image/png", pequeno)));
        BufferedImage b = lerPng(service.normalizarUpload(new MockMultipartFile("arquivo", "b.png", "image/png", grande)));

        assertThat(a.getWidth()).isEqualTo(900);
        assertThat(b.getWidth()).isEqualTo(900);
        int larguraA = larguraDoTraco(a);
        int larguraB = larguraDoTraco(b);
        assertThat(Math.abs(larguraA - larguraB)).isLessThanOrEqualTo((int) Math.ceil(larguraB * 0.02));
        // o traco preenche a largura util (900 menos 4% de cada lado)
        assertThat(larguraB).isBetween(810, 830);
    }

    @Test
    void normalizar_imagemLargaEImagemAlta_naoSaoEsticadas() throws IOException {
        byte[] larga = criarTraco(1000, 400, 50, 150, 900, 60);
        byte[] alta = criarTraco(400, 1000, 150, 50, 60, 900);

        BufferedImage l = lerPng(service.normalizarUpload(new MockMultipartFile("arquivo", "l.png", "image/png", larga)));
        BufferedImage t = lerPng(service.normalizarUpload(new MockMultipartFile("arquivo", "t.png", "image/png", alta)));

        int[] caixaL = AssinaturaImagemService.caixaDoTraco(l);
        int[] caixaT = AssinaturaImagemService.caixaDoTraco(t);
        double proporcaoL = (caixaL[2] - caixaL[0] + 1.0) / (caixaL[3] - caixaL[1] + 1.0);
        double proporcaoT = (caixaT[2] - caixaT[0] + 1.0) / (caixaT[3] - caixaT[1] + 1.0);
        assertThat(proporcaoL).isCloseTo(900.0 / 60, org.assertj.core.data.Percentage.withPercentage(8));
        assertThat(proporcaoT).isCloseTo(60.0 / 900, org.assertj.core.data.Percentage.withPercentage(8));
        // a alta fica centralizada na horizontal, ocupando a altura util
        assertThat(caixaT[3] - caixaT[1] + 1).isBetween(270, 280);
        assertThat((caixaT[0] + caixaT[2]) / 2).isBetween(440, 460);
    }

    @Test
    void normalizarBase64_quandoRecebeDataUrl_devolvePngValido()
            throws IOException {

        String base64 = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(
                criarAssinaturaPng(300, 100));

        byte[] resultado = service.normalizarBase64(base64);

        assertThat(ImageIO.read(
                new ByteArrayInputStream(resultado))).isNotNull();
    }

    @Test
    void normalizarBase64_quandoRecebeBase64Puro_devolvePngValido()
            throws IOException {

        String base64 = Base64.getEncoder().encodeToString(
                criarAssinaturaPng(300, 100));

        byte[] resultado = service.normalizarBase64(base64);

        assertThat(ImageIO.read(
                new ByteArrayInputStream(resultado))).isNotNull();
    }

    @Test
    void normalizarUpload_quandoArquivoVazio_devolveErroDeValidacao() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "assinatura.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.normalizarUpload(arquivo))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void normalizarUpload_quandoFormatoInvalido_devolveErroDeValidacao() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                "image/png",
                "isto nao e uma imagem".getBytes());

        assertThatThrownBy(() -> service.normalizarUpload(arquivo))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void normalizarUpload_quandoArquivoExcedeLimite_devolveErroDeValidacao() {
        AssinaturaImagemService servicoComLimitePequeno =
                new AssinaturaImagemService(64, 1200, 600);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                "image/png",
                new byte[65]);

        assertThatThrownBy(
                () -> servicoComLimitePequeno.normalizarUpload(arquivo))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void normalizarBase64_quandoConteudoInvalido_devolveErroDeValidacao() {
        assertThatThrownBy(
                () -> service.normalizarBase64("conteudo-invalido"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void normalizarUpload_quandoImagemTransparenteSemTracos_rejeita()
            throws IOException {

        BufferedImage imagemVazia = new BufferedImage(
                160, 80, BufferedImage.TYPE_INT_ARGB);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagemVazia, "png", saida);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                "image/png",
                saida.toByteArray());

        assertThatThrownBy(() -> service.normalizarUpload(arquivo))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Desenhe ou envie uma assinatura.");
    }

    private static BufferedImage lerPng(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    private static int larguraDoTraco(BufferedImage imagem) {
        int[] caixa = AssinaturaImagemService.caixaDoTraco(imagem);
        return caixa[2] - caixa[0] + 1;
    }

    /** Quadro transparente com um retangulo preto (o "traco") na posicao e no tamanho pedidos. */
    private static byte[] criarTraco(int largura, int altura, int x, int y, int w, int h) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graficos = imagem.createGraphics();
        try {
            graficos.setColor(Color.BLACK);
            graficos.fillRect(x, y, w, h);
        } finally {
            graficos.dispose();
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }

    private long contarPixelsOpacos(BufferedImage imagem) {
        long total = 0;

        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                if (new Color(imagem.getRGB(x, y), true).getAlpha() == 255) {
                    total++;
                }
            }
        }

        return total;
    }

    private byte[] criarAssinaturaPng(int largura, int altura)
            throws IOException {

        BufferedImage imagem = new BufferedImage(
                largura, altura, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graficos = imagem.createGraphics();

        try {
            graficos.setColor(Color.BLACK);
            graficos.setStroke(new BasicStroke(4));

            graficos.drawLine(
                    15, altura / 2,
                    largura / 2, altura / 3);

            graficos.drawLine(
                    largura / 2, altura / 3,
                    largura - 15, altura / 2);
        } finally {
            graficos.dispose();
        }

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);

        return saida.toByteArray();
    }

    private byte[] criarJpeg(int largura, int altura)
            throws IOException {

        BufferedImage imagem = new BufferedImage(
                largura, altura, BufferedImage.TYPE_INT_RGB);

        Graphics2D graficos = imagem.createGraphics();

        try {
            graficos.setColor(Color.WHITE);
            graficos.fillRect(0, 0, largura, altura);
            graficos.setColor(Color.BLACK);
            graficos.drawLine(10, altura / 2, largura - 10, altura / 2);
        } finally {
            graficos.dispose();
        }

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);

        return saida.toByteArray();
    }
}