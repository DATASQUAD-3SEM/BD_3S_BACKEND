package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

class CarimboAssinaturaPdfTest {

    private final CarimboAssinaturaPdf carimbo = new CarimboAssinaturaPdf();

    @Test
    void comAsDuasAssinaturas_colocaDuasImagensNaPagina() throws IOException {
        byte[] png = PdfsDeTeste.assinaturaPng();

        byte[] resultado = carimbo.aplicar(
                PdfsDeTeste.pdfComRotulos(true, true),
                Map.of(AssinaturaGuiaPapel.MEDICO, png, AssinaturaGuiaPapel.CHEFE, png));

        assertThat(contarImagens(resultado)).isEqualTo(2);
    }

    @Test
    void soComAAssinaturaDoChefe_colocaUmaImagem() throws IOException {
        byte[] png = PdfsDeTeste.assinaturaPng();

        byte[] resultado = carimbo.aplicar(
                PdfsDeTeste.pdfComRotulos(true, true),
                Map.of(AssinaturaGuiaPapel.CHEFE, png));

        assertThat(contarImagens(resultado)).isEqualTo(1);
    }

    @Test
    void semAssinaturas_devolveOPdfSemImagens() throws IOException {
        byte[] resultado = carimbo.aplicar(PdfsDeTeste.pdfComRotulos(true, true), Map.of());

        assertThat(contarImagens(resultado)).isZero();
    }

    @Test
    void rotuloAusente_lancaRegraDeNegocio() throws IOException {
        byte[] png = PdfsDeTeste.assinaturaPng();
        byte[] pdfSemRotuloMedico = PdfsDeTeste.pdfComRotulos(false, true);

        assertThatThrownBy(() -> carimbo.aplicar(
                pdfSemRotuloMedico, Map.of(AssinaturaGuiaPapel.MEDICO, png)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Assinatura Auditoria");
    }

    private static long contarImagens(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDResources recursos = documento.getPage(0).getResources();
            long total = 0;
            for (COSName nome : recursos.getXObjectNames()) {
                if (recursos.getXObject(nome) instanceof PDImageXObject) {
                    total++;
                }
            }
            return total;
        }
    }
}