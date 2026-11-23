package fatec.fusex.nexus.common.seed;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * T28-BE2 (SCRUM-249): arquivos de exemplo das faturas do seed de desenvolvimento. Gera o PDF de uma Guia de exemplo,
 * no mesmo formato lido pelo setor de Guias (numero, NAO RECEBER APOS, UG, paciente, Prec CP e procedimentos) e com
 * os tres rotulos onde as assinaturas sao carimbadas ("Assinatura Auditoria Previa Med/Dent", "Chefe do FuSEx" e
 * "Assinatura do Responsavel:"), e a imagem PNG de uma assinatura de exemplo. Nada disso e usado fora do seed.
 */
public final class ArquivosExemplo {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ArquivosExemplo() {
    }

    /** Procedimento de uma Guia de exemplo: Cod DGP, descricao e quantidade autorizada. */
    public record Procedimento(String codDgp, String descricao, int quantidade) {
    }

    public static byte[] pdfGuia(String numero, LocalDate validadeAte, String paciente, String precCp,
                                 List<Procedimento> procedimentos) {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                float y = 800;
                y = linha(conteudo, negrito, 11, y, "DOCUMENTO FICTÍCIO – GUIA DE EXEMPLO DO SEED DO NEXUS");
                y = linha(conteudo, fonte, 10, y, "UG: Cmdo Bda Inf Amv");
                y = linha(conteudo, negrito, 12, y, "GUIA DE ENCAMINHAMENTO Nr: " + numero + " / FUSEX");
                y = linha(conteudo, fonte, 10, y, "NÃO RECEBER APÓS: " + validadeAte.format(DATA));
                y = linha(conteudo, fonte, 10, y, "Beneficiário:");
                y = linha(conteudo, fonte, 10, y, "Titular: (PACIENTE)");
                y = linha(conteudo, fonte, 10, y, paciente.toUpperCase());
                y = linha(conteudo, fonte, 10, y, "UG Origem: Cmdo Bda Inf Amv");
                y = linha(conteudo, fonte, 10, y, "Prec CP:");
                y = linha(conteudo, fonte, 10, y, precCp);
                escrever(conteudo, fonte, 9, 330, y - 6, "Assinatura do Responsável:");
                y -= 46;
                y = linha(conteudo, fonte, 10, y, "Procedimentos: Cod DGP: Quant V.Unit Pós-Aud:");
                for (Procedimento p : procedimentos) {
                    y = linha(conteudo, fonte, 10, y, p.descricao() + " " + p.codDgp() + " " + p.quantidade());
                }
                linha(conteudo, fonte, 10, y, "Cotista: DAP , Credito: FUSEX OCS/C , ND: 339039");
                escrever(conteudo, fonte, 9, 60, 150, "Assinatura Auditoria Prévia Med/Dent");
                escrever(conteudo, fonte, 9, 360, 150, "Chefe do FuSEx");
            }
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o PDF de exemplo da Guia " + numero, e);
        }
    }

    public static byte[] pngAssinatura(String nome) {
        BufferedImage imagem = new BufferedImage(600, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(20, 30, 120));
        g.setFont(new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 56));
        g.drawString(nome, 30, 120);
        g.setStroke(new BasicStroke(3));
        g.drawLine(30, 140, 560, 150);
        g.dispose();
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar a assinatura de exemplo", e);
        }
    }

    private static float linha(PDPageContentStream conteudo, PDType1Font fonte, float tamanho, float y, String texto)
            throws IOException {
        escrever(conteudo, fonte, tamanho, 40, y, texto);
        return y - 15;
    }

    private static void escrever(PDPageContentStream conteudo, PDType1Font fonte, float tamanho, float x, float y,
                                 String texto) throws IOException {
        conteudo.beginText();
        conteudo.setFont(fonte, tamanho);
        conteudo.newLineAtOffset(x, y);
        conteudo.showText(texto);
        conteudo.endText();
    }
}
