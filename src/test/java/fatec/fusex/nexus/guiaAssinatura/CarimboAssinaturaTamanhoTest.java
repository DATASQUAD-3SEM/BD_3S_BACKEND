package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.common.seed.ArquivosExemplo;

/**
 * T37-BE2: as tres assinaturas saem do mesmo tamanho, sem deformacao, e nenhuma cobre o texto impresso da Guia.
 */
class CarimboAssinaturaTamanhoTest {

    private final CarimboAssinaturaPdf carimbo = new CarimboAssinaturaPdf();

    /** Guia no formato das Guias do seed e dos testes manuais, com os tres rotulos de assinatura. */
    private static byte[] guiaDeExemplo() {
        return ArquivosExemplo.pdfGuia("202612001", LocalDate.of(2099, 12, 31), "Antonio Carlos Ferreira",
                "45872213300", List.of(new ArquivosExemplo.Procedimento("40304361", "Hemograma completo", 2)));
    }

    @Test
    void tresAssinaturas_saemComAMesmaAlturaESemDeformacao() throws IOException {
        byte[] traco = assinatura(600, 200, 40, 60, 520, 80); // traco 520 x 80 (6,5 : 1) num quadro 600 x 200

        byte[] pdf = carimbo.aplicar(guiaDeExemplo(),
                Map.of(AssinaturaGuiaPapel.MEDICO, traco, AssinaturaGuiaPapel.CHEFE, traco), traco);

        List<Rectangle2D> imagens = imagensDesenhadas(pdf);
        assertThat(imagens).hasSize(3);
        for (Rectangle2D imagem : imagens) {
            // o traco e mais largo que a caixa (140 x 45): ocupa a largura toda, com a proporcao do recorte
            assertThat(imagem.getWidth()).isCloseTo(140.0, within(0.5));
            assertThat(imagem.getWidth() / imagem.getHeight()).isCloseTo(520.0 / 80.0, within(0.05));
        }
        assertThat(imagens.get(0).getHeight()).isCloseTo(imagens.get(1).getHeight(), within(0.01));
        assertThat(imagens.get(1).getHeight()).isCloseTo(imagens.get(2).getHeight(), within(0.01));
    }

    @Test
    void assinaturaAntigaComMuitaMargem_saiDoMesmoTamanhoQueUmaNova() throws IOException {
        byte[] antiga = assinatura(1200, 600, 30, 30, 150, 50); // traco pequeno num canto de um quadro grande
        byte[] nova = assinatura(900, 300, 36, 12, 828, 276); // mesmo traco (3:1), ja normalizado

        Rectangle2D a = imagensDesenhadas(carimbo.aplicar(guiaDeExemplo(), Map.of(AssinaturaGuiaPapel.CHEFE, antiga)))
                .get(0);
        Rectangle2D b = imagensDesenhadas(carimbo.aplicar(guiaDeExemplo(), Map.of(AssinaturaGuiaPapel.CHEFE, nova)))
                .get(0);

        assertThat(a.getHeight()).isCloseTo(b.getHeight(), within(1.5));
        assertThat(a.getWidth()).isCloseTo(b.getWidth(), within(1.5));
    }

    @Test
    void nenhumaAssinaturaCobreOTextoImpresso() throws IOException {
        byte[] traco = assinatura(900, 300, 36, 12, 828, 276); // traco alto: usa toda a altura da caixa

        byte[] pdf = carimbo.aplicar(guiaDeExemplo(),
                Map.of(AssinaturaGuiaPapel.MEDICO, traco, AssinaturaGuiaPapel.CHEFE, traco), traco);

        List<Rectangle2D> textos = retangulosDoTexto(pdf);
        for (Rectangle2D imagem : imagensDesenhadas(pdf)) {
            for (Rectangle2D texto : textos) {
                assertThat(imagem.intersects(texto))
                        .as("assinatura %s cobre o texto em %s", imagem, texto)
                        .isFalse();
            }
        }
    }

    @Test
    void textoLogoAcimaDoRotulo_reduzSoAAlturaDaCaixa() throws IOException {
        // "Chefe do FuSEx" em y=300 e uma linha impressa 40 pontos acima, na mesma faixa horizontal
        byte[] guia = pdfComLinhaAcimaDoChefe();
        byte[] traco = assinatura(900, 300, 36, 12, 828, 276);

        Rectangle2D imagem = imagensDesenhadas(carimbo.aplicar(guia, Map.of(AssinaturaGuiaPapel.CHEFE, traco))).get(0);

        assertThat(imagem.getHeight()).isLessThan(45.0).isGreaterThanOrEqualTo(CarimboAssinaturaPdf.ALTURA_MINIMA);
        for (Rectangle2D texto : retangulosDoTexto(guia)) {
            assertThat(imagem.intersects(texto)).isFalse();
        }
    }

    // ===== apoio =====

    private static byte[] pdfComLinhaAcimaDoChefe() throws IOException {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            documento.addPage(pagina);
            var fonte = new org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
            try (var conteudo = new org.apache.pdfbox.pdmodel.PDPageContentStream(documento, pagina)) {
                for (Object[] linha : new Object[][] {{340f, "Linha impressa acima do campo do chefe"}, {300f, "Chefe do FuSEx"}}) {
                    conteudo.beginText();
                    conteudo.setFont(fonte, 10);
                    conteudo.newLineAtOffset(340, (float) linha[0]);
                    conteudo.showText((String) linha[1]);
                    conteudo.endText();
                }
            }
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    /** Quadro transparente com um retangulo azul (o traco) na posicao pedida. */
    private static byte[] assinatura(int largura, int altura, int x, int y, int w, int h) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setColor(new Color(20, 30, 120));
            g.fillRect(x, y, w, h);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        }
    }

    /** Retangulos (em pontos, origem embaixo) das imagens desenhadas na primeira pagina, na ordem de desenho. */
    private static List<Rectangle2D> imagensDesenhadas(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            ColetorDeImagens coletor = new ColetorDeImagens(documento.getPage(0));
            coletor.processPage(documento.getPage(0));
            return coletor.imagens;
        }
    }

    /** Retangulo de cada glifo impresso (em pontos, origem embaixo), com a parte de baixo das letras. */
    private static List<Rectangle2D> retangulosDoTexto(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            float alturaDaPagina = documento.getPage(0).getMediaBox().getHeight();
            List<Rectangle2D> retangulos = new ArrayList<>();
            PDFTextStripper leitor = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition glifo) {
                    if (glifo.getUnicode() == null || glifo.getUnicode().isBlank()) {
                        return;
                    }
                    float base = alturaDaPagina - glifo.getYDirAdj();
                    float descida = glifo.getFontSizeInPt() * 0.2f;
                    retangulos.add(new Rectangle2D.Float(glifo.getXDirAdj(), base - descida,
                            glifo.getWidthDirAdj(), glifo.getHeightDir() + descida));
                }
            };
            leitor.getText(documento);
            return retangulos;
        }
    }

    private static final class ColetorDeImagens extends PDFGraphicsStreamEngine {

        final List<Rectangle2D> imagens = new ArrayList<>();

        ColetorDeImagens(PDPage pagina) {
            super(pagina);
        }

        @Override
        public void drawImage(PDImage pdImage) {
            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
            imagens.add(new Rectangle2D.Float(ctm.getTranslateX(), ctm.getTranslateY(),
                    ctm.getScalingFactorX(), ctm.getScalingFactorY()));
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        }

        @Override
        public void clip(int windingRule) {
        }

        @Override
        public void moveTo(float x, float y) {
        }

        @Override
        public void lineTo(float x, float y) {
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float();
        }

        @Override
        public void closePath() {
        }

        @Override
        public void endPath() {
        }

        @Override
        public void strokePath() {
        }

        @Override
        public void fillPath(int windingRule) {
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
        }

        @Override
        public void shadingFill(org.apache.pdfbox.cos.COSName shadingName) {
        }
    }
}
