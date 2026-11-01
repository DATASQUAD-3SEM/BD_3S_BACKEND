package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import fatec.fusex.nexus.assinatura.Assinatura;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Testes da montagem do PDF da Guia compartilhada pelos endpoints do medico, do chefe, do beneficiario e da recepcao.
 */
class MontagemPdfGuiaTest {

    private AssinaturaGuiaRepository assinaturaGuiaRepository;
    private ArmazenamentoArquivo armazenamentoArquivo;
    private MontagemPdfGuia montagem;
    private byte[] pdfOriginal;
    private Guia guia;

    @BeforeEach
    void preparar() throws IOException {
        assinaturaGuiaRepository = mock(AssinaturaGuiaRepository.class);
        armazenamentoArquivo = mock(ArmazenamentoArquivo.class);
        montagem = new MontagemPdfGuia(assinaturaGuiaRepository, armazenamentoArquivo, new CarimboAssinaturaPdf());

        pdfOriginal = PdfsDeTeste.pdfComRotulos(true, true);
        guia = new Guia();
        guia.setIdGuia(1L);
        guia.setArquivoUrl("guias/guia.pdf");
        when(armazenamentoArquivo.carregar("guias/guia.pdf")).thenReturn(new ByteArrayResource(pdfOriginal));
        when(armazenamentoArquivo.carregar("assinaturas/u.png"))
                .thenReturn(new ByteArrayResource(PdfsDeTeste.assinaturaPng()));
    }

    @Test
    void semAssinaturas_devolveOPdfOriginal() {
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(1L))).thenReturn(List.of());

        assertThat(montagem.montar(guia)).isEqualTo(pdfOriginal);
    }

    @Test
    void comAssinaturas_devolvePdfValidoDiferenteDoOriginal() throws IOException {
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(1L))).thenReturn(List.of(
                assinatura(AssinaturaGuiaPapel.MEDICO, "assinaturas/u.png"),
                assinatura(AssinaturaGuiaPapel.CHEFE, "assinaturas/u.png")));

        byte[] montado = montagem.montar(guia);

        assertThat(montado).isNotEqualTo(pdfOriginal);
        try (PDDocument documento = Loader.loadPDF(montado)) {
            assertThat(documento.getNumberOfPages()).isPositive();
        }
    }


    private static AssinaturaGuia assinatura(AssinaturaGuiaPapel papel, String imagem) {
        Assinatura vigente = new Assinatura();
        vigente.setAssinaturaUrl(imagem);

        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setPapel(papel);
        assinatura.setAssinatura(vigente);
        return assinatura;
    }
}
