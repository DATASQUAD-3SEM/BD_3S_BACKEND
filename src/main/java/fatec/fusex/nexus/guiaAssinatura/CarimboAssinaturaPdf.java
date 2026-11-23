package fatec.fusex.nexus.guiaAssinatura;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.usuario.AssinaturaImagemService;

/**
 * Aplica as imagens das assinaturas (medico e chefe) sobre as linhas de assinatura do PDF da Guia.
 * Linhas sem assinatura ficam em branco. O PDF original armazenado nao e alterado.
 *
 * T37-BE2: as tres assinaturas (medico, chefe e responsavel) saem do mesmo tamanho e sem deformacao. Cada imagem e
 * recortada em memoria na caixa do traco (vale tambem para as assinaturas gravadas antes da T37-BE1) e encaixada,
 * mantendo a proporcao, numa caixa de 140 x 45 pontos, centralizada sobre o rotulo, com a base 2 pontos acima dele.
 * Se o texto impresso acima do rotulo deixar menos de 45 pontos livres, so a altura da caixa diminui.
 */
@Component
public class CarimboAssinaturaPdf {

    static final float LARGURA_ASSINATURA = 140f;
    static final float ALTURA_ASSINATURA = 45f;
    /** Distancia entre a base da assinatura e o topo do rotulo. */
    static final float ACIMA_DO_ROTULO = 2f;
    /** Folga minima entre o topo da assinatura e o texto impresso acima dela. */
    static final float FOLGA_TEXTO_ACIMA = 2f;
    /** Mesmo com pouco espaco, a assinatura nao fica menor que isto. */
    static final float ALTURA_MINIMA = 16f;

    static final Map<AssinaturaGuiaPapel, String> ROTULOS = Map.of(
            AssinaturaGuiaPapel.MEDICO, "Assinatura Auditoria Pr\u00e9via Med/Dent",
            AssinaturaGuiaPapel.CHEFE, "Chefe do FuSEx");

    // T23-BE1: campo do beneficiario (ou do responsavel legal pelo menor).
    static final String ROTULO_RESPONSAVEL = "Assinatura do Respons\u00e1vel";

    public byte[] aplicar(byte[] pdfOriginal, Map<AssinaturaGuiaPapel, byte[]> assinaturas) {
        return aplicar(pdfOriginal, assinaturas, null);
    }

    /**
     * Aplica as assinaturas do medico e do chefe e, se {@code assinaturaResponsavel} nao for nulo, a do beneficiario
     * no campo "Assinatura do Responsavel". Com a assinatura do beneficiario, a linha precisa existir no PDF.
     */
    public byte[] aplicar(byte[] pdfOriginal, Map<AssinaturaGuiaPapel, byte[]> assinaturas,
                          byte[] assinaturaResponsavel) {
        try (PDDocument documento = Loader.loadPDF(pdfOriginal)) {
            List<List<TextPosition>> paginas = coletarGlifos(documento);

            for (Map.Entry<AssinaturaGuiaPapel, byte[]> assinatura : assinaturas.entrySet()) {
                String rotuloEsperado = ROTULOS.get(assinatura.getKey());
                Rotulo rotulo = localizar(paginas, rotuloEsperado)
                        .orElseThrow(() -> new RegraDeNegocioException(
                                "PDF da guia sem a linha de assinatura: " + rotuloEsperado));

                carimbar(documento, paginas.get(rotulo.pagina()), rotulo, assinatura.getValue());
            }

            if (assinaturaResponsavel != null) {
                Rotulo rotulo = localizar(paginas, ROTULO_RESPONSAVEL)
                        .orElseThrow(() -> new RegraDeNegocioException(
                                "PDF da guia sem a linha de assinatura: " + ROTULO_RESPONSAVEL));
                carimbar(documento, paginas.get(rotulo.pagina()), rotulo, assinaturaResponsavel);
            }

            try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
                documento.save(saida);
                return saida.toByteArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao aplicar as assinaturas na guia", e);
        }
    }

    private static List<List<TextPosition>> coletarGlifos(PDDocument documento) throws IOException {
        ColetorDeGlifos coletor = new ColetorDeGlifos();
        coletor.getText(documento);
        return coletor.paginas;
    }

    private static Optional<Rotulo> localizar(List<List<TextPosition>> paginas, String rotulo) {
        String alvo = semEspacos(rotulo);

        for (int p = 0; p < paginas.size(); p++) {
            List<TextPosition> glifos = new ArrayList<>();
            StringBuilder texto = new StringBuilder();

            for (TextPosition glifo : paginas.get(p)) {
                String unicode = glifo.getUnicode();
                if (unicode == null) {
                    continue;
                }
                for (char c : normalizar(unicode).toCharArray()) {
                    if (!Character.isWhitespace(c)) {
                        texto.append(c);
                        glifos.add(glifo);
                    }
                }
            }

            int inicio = texto.indexOf(alvo);
            if (inicio >= 0) {
                return Optional.of(Rotulo.de(p, glifos.subList(inicio, inicio + alvo.length())));
            }
        }
        return Optional.empty();
    }

    /** Assinatura de qualquer papel: recortada no traco e encaixada na caixa padrao sobre o rotulo. */
    private static void carimbar(PDDocument documento, List<TextPosition> glifosDaPagina, Rotulo rotulo,
                                 byte[] imagem) throws IOException {
        PDPage pagina = documento.getPage(rotulo.pagina());
        float alturaDaPagina = pagina.getMediaBox().getHeight();

        BufferedImage traco = recortarNoTraco(imagem);
        float centro = (rotulo.x0() + rotulo.x1()) / 2f;
        // Coordenadas de cima para baixo (as do texto extraido): a base da assinatura fica 2 pontos acima do rotulo.
        float baseDaCaixa = rotulo.topo() - ACIMA_DO_ROTULO;
        float livre = espacoLivreAcima(glifosDaPagina, rotulo, centro - LARGURA_ASSINATURA / 2f,
                centro + LARGURA_ASSINATURA / 2f, baseDaCaixa);
        float alturaDaCaixa = Math.max(ALTURA_MINIMA, Math.min(ALTURA_ASSINATURA, livre));

        float escala = Math.min(LARGURA_ASSINATURA / traco.getWidth(), alturaDaCaixa / traco.getHeight());
        float largura = traco.getWidth() * escala;
        float altura = traco.getHeight() * escala;

        PDImageXObject figura = LosslessFactory.createFromImage(documento, traco);
        try (PDPageContentStream conteudo = new PDPageContentStream(
                documento, pagina, PDPageContentStream.AppendMode.APPEND, true, true)) {
            conteudo.drawImage(figura, centro - largura / 2f, alturaDaPagina - baseDaCaixa, largura, altura);
        }
    }

    /**
     * Altura livre entre a base da caixa e o texto impresso mais baixo que fica acima do rotulo e cruza a faixa
     * horizontal da caixa. Sem texto acima, a altura livre e a distancia ate o topo da pagina.
     */
    static float espacoLivreAcima(List<TextPosition> glifos, Rotulo rotulo, float x0, float x1, float baseDaCaixa) {
        float limite = 0f;
        for (TextPosition glifo : glifos) {
            float linhaDeBase = glifo.getYDirAdj();
            boolean acimaDoRotulo = linhaDeBase < rotulo.topo();
            boolean cruzaAFaixa = glifo.getXDirAdj() < x1 && glifo.getXDirAdj() + glifo.getWidthDirAdj() > x0;
            if (acimaDoRotulo && cruzaAFaixa) {
                // parte de baixo das letras (descendentes): cerca de 1/4 do tamanho da fonte abaixo da linha de base
                limite = Math.max(limite, linhaDeBase + glifo.getFontSizeInPt() * 0.25f);
            }
        }
        return baseDaCaixa - limite - FOLGA_TEXTO_ACIMA;
    }

    /** Le a imagem e devolve so o retangulo do traco; sem traco reconhecivel, a imagem inteira. */
    static BufferedImage recortarNoTraco(byte[] imagem) throws IOException {
        BufferedImage lida = ImageIO.read(new ByteArrayInputStream(imagem));
        if (lida == null) {
            throw new IOException("Imagem da assinatura ilegivel");
        }
        BufferedImage argb = new BufferedImage(lida.getWidth(), lida.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var g = argb.createGraphics();
        try {
            g.drawImage(lida, 0, 0, null);
        } finally {
            g.dispose();
        }
        int[] caixa = AssinaturaImagemService.caixaDoTraco(argb);
        if (caixa == null) {
            return argb;
        }
        return argb.getSubimage(caixa[0], caixa[1], caixa[2] - caixa[0] + 1, caixa[3] - caixa[1] + 1);
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFC);
    }

    private static String semEspacos(String texto) {
        StringBuilder limpo = new StringBuilder();
        for (char c : normalizar(texto).toCharArray()) {
            if (!Character.isWhitespace(c)) {
                limpo.append(c);
            }
        }
        return limpo.toString();
    }

    record Rotulo(int pagina, float x0, float x1, float topo, float base) {

        static Rotulo de(int pagina, List<TextPosition> glifos) {
            float x0 = Float.MAX_VALUE;
            float x1 = -Float.MAX_VALUE;
            float topo = Float.MAX_VALUE;

            for (TextPosition glifo : glifos) {
                x0 = Math.min(x0, glifo.getXDirAdj());
                x1 = Math.max(x1, glifo.getXDirAdj() + glifo.getWidthDirAdj());
                topo = Math.min(topo, glifo.getYDirAdj() - glifo.getHeightDir());
            }
            float base = glifos.get(0).getYDirAdj();
            return new Rotulo(pagina, x0, x1, topo, base);
        }
    }

    private static final class ColetorDeGlifos extends PDFTextStripper {

        final List<List<TextPosition>> paginas = new ArrayList<>();

        ColetorDeGlifos() throws IOException {
            super();
        }

        @Override
        protected void startPage(PDPage pagina) throws IOException {
            paginas.add(new ArrayList<>());
            super.startPage(pagina);
        }

        @Override
        protected void processTextPosition(TextPosition glifo) {
            if (paginas.isEmpty()) {
                paginas.add(new ArrayList<>());
            }
            paginas.get(paginas.size() - 1).add(glifo);
        }
    }
}