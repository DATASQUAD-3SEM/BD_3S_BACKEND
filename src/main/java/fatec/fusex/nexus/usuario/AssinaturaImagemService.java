package fatec.fusex.nexus.usuario;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;

/**
 * Valida e normaliza a imagem da assinatura digital (T13-BE2): aceita PNG ou JPG, rejeita imagem em branco
 * (422), aplica fundo transparente e limita o tamanho maximo.
 *
 * T37-BE1: a imagem e recortada na caixa do traco (pixels transparentes e quase brancos ficam fora), recebe uma
 * margem fixa de 4% de cada lado e e centralizada, sem esticar, numa imagem PNG transparente de 900 x 300 pixels.
 */
@Service
public class AssinaturaImagemService {

    /** Tamanho fixo de toda assinatura guardada (proporcao 3:1). */
    static final int LARGURA_FINAL = 900;
    static final int ALTURA_FINAL = 300;
    /** Margem de cada lado, em fracao da largura e da altura finais. */
    static final double MARGEM = 0.04;

    static final String MSG_SEM_TRACO = "Desenhe ou envie uma assinatura.";

    private final long limiteBytes;
    private final int larguraMaxima;
    private final int alturaMaxima;

    public AssinaturaImagemService(
            @Value("${nexus.assinatura.max-bytes:2097152}")
            long limiteBytes,
            @Value("${nexus.assinatura.max-width:1200}")
            int larguraMaxima,
            @Value("${nexus.assinatura.max-height:600}")
            int alturaMaxima) {

        if (limiteBytes <= 0 || larguraMaxima <= 0 || alturaMaxima <= 0) {
            throw new IllegalArgumentException(
                    "Os limites da assinatura devem ser positivos");
        }

        this.limiteBytes = limiteBytes;
        this.larguraMaxima = larguraMaxima;
        this.alturaMaxima = alturaMaxima;
    }

    public byte[] normalizarUpload(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw erro("Envie uma imagem de assinatura");
        }

        if (arquivo.getSize() > limiteBytes) {
            throw erro("A imagem excede o tamanho máximo permitido");
        }

        try {
            return normalizar(arquivo.getBytes(), arquivo.getContentType());
        } catch (IOException ex) {
            throw erro("Não foi possível ler a imagem enviada");
        }
    }

    public byte[] normalizarBase64(String imagemBase64) {
        if (imagemBase64 == null || imagemBase64.isBlank()) {
            throw erro("Envie uma imagem de assinatura");
        }

        String conteudo = imagemBase64.trim();
        String mimeDeclarado = null;

        if (conteudo.startsWith("data:")) {
            int separador = conteudo.indexOf(',');

            if (separador < 0) {
                throw erro("Imagem Base64 inválida");
            }

            String cabecalho = conteudo.substring(0, separador);
            String cabecalhoNormalizado =
                    cabecalho.toLowerCase(Locale.ROOT);

            if (!cabecalhoNormalizado.endsWith(";base64")) {
                throw erro("A imagem Base64 deve estar codificada em Base64");
            }

            mimeDeclarado = cabecalho.substring(
                    "data:".length(),
                    cabecalho.indexOf(';'));

            if (!mimeDeclarado.equalsIgnoreCase("image/png")
                    && !mimeDeclarado.equalsIgnoreCase("image/jpeg")) {
                throw erro("Formato inválido. Envie uma imagem PNG ou JPG");
            }

            conteudo = conteudo.substring(separador + 1);
        }

        // Evita decodificar uma string que já ultrapassa o limite permitido.
        long tamanhoMaximoBase64 = ((limiteBytes + 2L) / 3L) * 4L + 128L;

        if (conteudo.length() > tamanhoMaximoBase64) {
            throw erro("A imagem excede o tamanho máximo permitido");
        }

        byte[] bytes;

        try {
            bytes = Base64.getDecoder().decode(
                    conteudo.replaceAll("\\s", ""));
        } catch (IllegalArgumentException ex) {
            throw erro("Imagem Base64 inválida");
        }

        return normalizar(bytes, mimeDeclarado);
    }

    private byte[] normalizar(byte[] bytes, String mimeDeclarado) {
        if (bytes == null || bytes.length == 0) {
            throw erro("A imagem enviada está vazia");
        }

        if (bytes.length > limiteBytes) {
            throw erro("A imagem excede o tamanho máximo permitido");
        }

        validarMimeDeclarado(mimeDeclarado);

        try (ImageInputStream entrada = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {

            if (entrada == null) {
                throw erro("Arquivo de imagem inválido");
            }

            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);

            if (!leitores.hasNext()) {
                throw erro("Formato inválido. Envie uma imagem PNG ou JPG");
            }

            ImageReader leitor = leitores.next();

            try {
                leitor.setInput(entrada, true, true);

                String formato = leitor.getFormatName()
                        .toLowerCase(Locale.ROOT);

                if (!formato.equals("png")
                        && !formato.equals("jpg")
                        && !formato.equals("jpeg")) {
                    throw erro("Formato inválido. Envie uma imagem PNG ou JPG");
                }

                validarCompatibilidadeMime(mimeDeclarado, formato);

                int larguraOriginal = leitor.getWidth(0);
                int alturaOriginal = leitor.getHeight(0);

                if (larguraOriginal <= 0 || alturaOriginal <= 0) {
                    throw erro("As dimensões da imagem são inválidas");
                }


                double fatorReducao = Math.max(
                        (double) larguraOriginal / larguraMaxima,
                        (double) alturaOriginal / alturaMaxima);

                int amostragem = Math.max(
                        1, (int) Math.ceil(fatorReducao));

                var parametros = leitor.getDefaultReadParam();
                parametros.setSourceSubsampling(
                        amostragem, amostragem, 0, 0);

                BufferedImage imagem = leitor.read(0, parametros);

                if (imagem == null) {
                    throw erro("Não foi possível interpretar a imagem");
                }

                if (!contemAssinatura(imagem)) {
                    throw erro(MSG_SEM_TRACO);
                }

                // T37-BE1: todas as assinaturas saem no mesmo formato, so com o traco: o tamanho final nao depende
                // da area ocupada ao assinar (desenho pequeno num canto ou grande no centro dao o mesmo resultado).
                byte[] resultado = escreverPng(enquadrar(imagem));

                if (resultado.length > limiteBytes) {
                    throw erro("Não foi possível adequar a imagem ao limite permitido");
                }

                return resultado;

            } finally {
                leitor.dispose();
            }

        } catch (IOException ex) {
            throw erro("Não foi possível processar a imagem enviada");
        }
    }

    /**
     * Converte para ARGB com o fundo claro transparente, recorta na caixa do traco e centraliza o recorte, mantendo
     * a proporcao, na area util (tamanho final menos as margens) de uma imagem de 900 x 300.
     */
    static BufferedImage enquadrar(BufferedImage lida) {
        BufferedImage argb = new BufferedImage(lida.getWidth(), lida.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D copia = argb.createGraphics();
        try {
            copia.setComposite(AlphaComposite.Src);
            copia.drawImage(lida, 0, 0, null);
        } finally {
            copia.dispose();
        }
        tornarFundoClaroTransparente(argb);

        int[] caixa = caixaDoTraco(argb);
        if (caixa == null) {
            throw new DadosInvalidosException("assinatura", MSG_SEM_TRACO);
        }
        BufferedImage traco = argb.getSubimage(caixa[0], caixa[1], caixa[2] - caixa[0] + 1, caixa[3] - caixa[1] + 1);

        double larguraUtil = LARGURA_FINAL * (1 - 2 * MARGEM);
        double alturaUtil = ALTURA_FINAL * (1 - 2 * MARGEM);
        double escala = Math.min(larguraUtil / traco.getWidth(), alturaUtil / traco.getHeight());
        int largura = Math.max(1, (int) Math.round(traco.getWidth() * escala));
        int altura = Math.max(1, (int) Math.round(traco.getHeight() * escala));

        BufferedImage resultado = new BufferedImage(LARGURA_FINAL, ALTURA_FINAL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graficos = resultado.createGraphics();
        try {
            graficos.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graficos.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graficos.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graficos.drawImage(traco, (LARGURA_FINAL - largura) / 2, (ALTURA_FINAL - altura) / 2, largura, altura, null);
        } finally {
            graficos.dispose();
        }
        return resultado;
    }

    /** Menor retangulo {x0, y0, x1, y1} que contem os pixels do traco, ou null se nao houver traco. */
    static int[] caixaDoTraco(BufferedImage imagem) {
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = -1;
        int y1 = -1;
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                if (ehTraco(imagem.getRGB(x, y))) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        return x1 < 0 ? null : new int[] {x0, y0, x1, y1};
    }

    /** Pixel do traco: nao transparente e nao quase branco (ignora sujeira clara de JPEG e o antisserrilhado fraco). */
    private static boolean ehTraco(int cor) {
        int alfa = (cor >>> 24) & 0xFF;
        int vermelho = (cor >>> 16) & 0xFF;
        int verde = (cor >>> 8) & 0xFF;
        int azul = cor & 0xFF;
        return alfa > 32 && Math.min(vermelho, Math.min(verde, azul)) < 225;
    }

    private static void tornarFundoClaroTransparente(BufferedImage imagem) {
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                int cor = imagem.getRGB(x, y);

                int alfa = (cor >>> 24) & 0xFF;
                int vermelho = (cor >>> 16) & 0xFF;
                int verde = (cor >>> 8) & 0xFF;
                int azul = cor & 0xFF;

                if (alfa > 0
                        && vermelho >= 245
                        && verde >= 245
                        && azul >= 245) {

                    imagem.setRGB(x, y, 0x00FFFFFF);
                }
            }
        }
    }

    private boolean contemAssinatura(BufferedImage imagem) {
        long totalPixels = (long) imagem.getWidth() * imagem.getHeight();
        long pixelsNecessarios = Math.max(3L, totalPixels / 100_000L);
        long pixelsVisiveis = 0;

        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                int cor = imagem.getRGB(x, y);

                int alfa = (cor >>> 24) & 0xFF;
                int vermelho = (cor >>> 16) & 0xFF;
                int verde = (cor >>> 8) & 0xFF;
                int azul = cor & 0xFF;

                // Ignora transparência e pixels próximos do branco.
                if (alfa > 16
                        && (vermelho < 245
                        || verde < 245
                        || azul < 245)) {

                    pixelsVisiveis++;

                    if (pixelsVisiveis >= pixelsNecessarios) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private byte[] escreverPng(BufferedImage imagem) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();

        if (!ImageIO.write(imagem, "png", saida)) {
            throw erro("Não foi possível gerar a imagem normalizada");
        }

        return saida.toByteArray();
    }

    private void validarMimeDeclarado(String mime) {
        if (mime == null || mime.isBlank()) {
            return;
        }

        String normalizado = mime
                .split(";")[0]
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!normalizado.equals("image/png")
                && !normalizado.equals("image/jpeg")
                && !normalizado.equals("image/jpg")
                && !normalizado.equals("application/octet-stream")) {
            throw erro("Formato inválido. Envie uma imagem PNG ou JPG");
        }
    }

    private void validarCompatibilidadeMime(String mime, String formato) {
        if (mime == null || mime.isBlank()
                || mime.equalsIgnoreCase("application/octet-stream")) {
            return;
        }

        String normalizado = mime.toLowerCase(Locale.ROOT);

        boolean pngCompativel = formato.equals("png")
                && normalizado.equals("image/png");

        boolean jpgCompativel = (formato.equals("jpg")
                || formato.equals("jpeg"))
                && (normalizado.equals("image/jpeg")
                || normalizado.equals("image/jpg"));

        if (!pngCompativel && !jpgCompativel) {
            throw erro("O tipo declarado não corresponde à imagem enviada");
        }
    }

    private DadosInvalidosException erro(String mensagem) {
        return new DadosInvalidosException("assinatura", mensagem);
    }
}