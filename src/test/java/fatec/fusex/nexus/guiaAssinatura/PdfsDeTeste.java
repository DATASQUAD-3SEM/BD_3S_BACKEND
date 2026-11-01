package fatec.fusex.nexus.guiaAssinatura;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Utilitario dos testes: gera PDFs com as linhas de assinatura e imagens PNG de assinatura em memoria.
 */
public final class PdfsDeTeste {

    private PdfsDeTeste() {
    }

    public static byte[] pdfComRotulos(boolean medico, boolean chefe) throws IOException {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {

            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                if (medico) {
                    escrever(conteudo, fonte, 60, 300, "Assinatura Auditoria Pr\u00e9via Med/Dent");
                }
                if (chefe) {
                    escrever(conteudo, fonte, 360, 300, "Chefe do FuSEx");
                }
            }

            documento.save(saida);
            return saida.toByteArray();
        }
    }

    public static byte[] assinaturaPng() throws IOException {
        BufferedImage imagem = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(5, 5, 100, 25);
        g.dispose();

        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        }
    }

    private static void escrever(PDPageContentStream conteudo, PDType1Font fonte,
                                 float x, float y, String texto) throws IOException {
        conteudo.beginText();
        conteudo.setFont(fonte, 10);
        conteudo.newLineAtOffset(x, y);
        conteudo.showText(texto);
        conteudo.endText();
    }
}