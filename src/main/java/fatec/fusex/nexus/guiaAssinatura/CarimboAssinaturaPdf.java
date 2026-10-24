package fatec.fusex.nexus.guiaAssinatura;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

@Component
public class CarimboAssinaturaPdf {

    static final float LARGURA_ASSINATURA = 140f;
    static final float ALTURA_ASSINATURA = 45f;
    static final float DISTANCIA_ACIMA_DO_ROTULO = 12f;

    static final Map<AssinaturaGuiaPapel, String> ROTULOS = Map.of(
            AssinaturaGuiaPapel.MEDICO, "Assinatura Auditoria Pr\u00e9via Med/Dent",
            AssinaturaGuiaPapel.CHEFE, "Chefe do FuSEx");

    public byte[] aplicar(byte[] pdfOriginal, Map<AssinaturaGuiaPapel, byte[]> assinaturas) {
        try (PDDocument documento = Loader.loadPDF(pdfOriginal)) {
            List<List<TextPosition>> paginas = coletarGlifos(documento);

            for (Map.Entry<AssinaturaGuiaPapel, byte[]> assinatura : assinaturas.entrySet()) {
                String rotuloEsperado = ROTULOS.get(assinatura.getKey());
                Rotulo rotulo = localizar(paginas, rotuloEsperado)
                        .orElseThrow(() -> new RegraDeNegocioException(
                                "PDF da guia sem a linha de assinatura: " + rotuloEsperado));

                carimbar(documento, rotulo, assinatura.getKey(), assinatura.getValue());
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

    private static void carimbar(PDDocument documento, Rotulo rotulo,
                                 AssinaturaGuiaPapel papel, byte[] imagem) throws IOException {
        PDPage pagina = documento.getPage(rotulo.pagina());
        float alturaDaPagina = pagina.getMediaBox().getHeight();

        float x = (rotulo.x0() + rotulo.x1()) / 2f - LARGURA_ASSINATURA / 2f;
        float y = alturaDaPagina - rotulo.topo() + DISTANCIA_ACIMA_DO_ROTULO;

        PDImageXObject figura = PDImageXObject.createFromByteArray(
                documento, imagem, "assinatura-" + papel.name().toLowerCase());

        try (PDPageContentStream conteudo = new PDPageContentStream(
                documento, pagina, PDPageContentStream.AppendMode.APPEND, true, true)) {
            conteudo.drawImage(figura, x, y, LARGURA_ASSINATURA, ALTURA_ASSINATURA);
        }
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

    private record Rotulo(int pagina, float x0, float x1, float topo) {

        static Rotulo de(int pagina, List<TextPosition> glifos) {
            float x0 = Float.MAX_VALUE;
            float x1 = -Float.MAX_VALUE;
            float topo = Float.MAX_VALUE;

            for (TextPosition glifo : glifos) {
                x0 = Math.min(x0, glifo.getXDirAdj());
                x1 = Math.max(x1, glifo.getXDirAdj() + glifo.getWidthDirAdj());
                topo = Math.min(topo, glifo.getYDirAdj() - glifo.getHeightDir());
            }
            return new Rotulo(pagina, x0, x1, topo);
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