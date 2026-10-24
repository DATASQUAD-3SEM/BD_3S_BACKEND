package fatec.fusex.nexus.guiaAssinatura;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;

class CarimboAssinaturaPdfVisualTest {

    @Test
    void geraPdfCarimbadoParaInspecaoVisual() throws IOException {
        Path modelo = Path.of("src/test/resources/guia-modelo.pdf");
        Assumptions.assumeTrue(Files.exists(modelo),
                "Copie o PDF modelo para src/test/resources/guia-modelo.pdf para rodar este teste");

        byte[] saida = new CarimboAssinaturaPdf().aplicar(
                Files.readAllBytes(modelo),
                Map.of(
                        AssinaturaGuiaPapel.MEDICO, imagem("MEDICO"),
                        AssinaturaGuiaPapel.CHEFE, imagem("CHEFE")));

        Path destino = Path.of("target/guia-carimbada.pdf");
        Files.createDirectories(destino.getParent());
        Files.write(destino, saida);

        System.out.println("PDF gerado em: " + destino.toAbsolutePath());
    }

    private static byte[] imagem(String texto) throws IOException {
        BufferedImage imagem = new BufferedImage(260, 90, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(Color.BLUE);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(texto, 10, 55);
        g.setColor(Color.RED);
        g.drawRect(0, 0, 259, 89);
        g.dispose();

        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        }
    }
}