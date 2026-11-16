package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;

/**
 * Testes da assinatura do beneficiario na montagem do PDF (T23-BE1): quando entra, qual imagem usa e o erro
 * quando o campo nao existe no PDF.
 */
class MontagemPdfGuiaBeneficiarioTest {

    private static final LocalDate PASSADA = LocalDate.of(2020, 1, 1);
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);

    private AssinaturaGuiaRepository assinaturaGuiaRepository;
    private SessaoRepository sessaoRepository;
    private ArmazenamentoArquivo armazenamentoArquivo;
    private MontagemPdfGuia montagem;
    private byte[] pdfComCampo;
    private Guia guia;

    @BeforeEach
    void preparar() throws IOException {
        assinaturaGuiaRepository = mock(AssinaturaGuiaRepository.class);
        sessaoRepository = mock(SessaoRepository.class);
        armazenamentoArquivo = mock(ArmazenamentoArquivo.class);
        montagem = new MontagemPdfGuia(assinaturaGuiaRepository, armazenamentoArquivo, new CarimboAssinaturaPdf(),
                sessaoRepository);

        pdfComCampo = PdfsDeTeste.pdfComRotulos(true, true, true);
        guia = new Guia();
        guia.setIdGuia(1L);
        guia.setArquivoUrl("guias/guia.pdf");
        guia.setDataValidadeAte(FUTURA);
        when(armazenamentoArquivo.carregar("guias/guia.pdf")).thenReturn(new ByteArrayResource(pdfComCampo));
        when(armazenamentoArquivo.carregar("assinaturas/beneficiario.png"))
                .thenReturn(new ByteArrayResource(PdfsDeTeste.assinaturaPng()));
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(any())).thenReturn(List.of());
    }

    @Test
    void guiaRealizada_insereAAssinaturaDoBeneficiario() throws IOException {
        comSessoes(
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 2, 10, 0), "assinaturas/beneficiario.png"),
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 3, 10, 0), "assinaturas/beneficiario.png"));

        assertThat(contarImagens(montagem.montar(guia))).isEqualTo(1);
    }

    @Test
    void guiaEncerrada_comUmaAssinadaEValidadePassada_insereAAssinatura() throws IOException {
        guia.setDataValidadeAte(PASSADA);
        comSessoes(
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 2, 10, 0), "assinaturas/beneficiario.png"),
                sessao(SituacaoSessao.EXPIRADA, null, null));

        assertThat(contarImagens(montagem.montar(guia))).isEqualTo(1);
    }

    @Test
    void guiaVencidaSemAssinatura_saiSemAAssinaturaDoBeneficiario() throws IOException {
        guia.setDataValidadeAte(PASSADA);
        comSessoes(sessao(SituacaoSessao.EXPIRADA, null, null));

        assertThat(montagem.montar(guia)).isEqualTo(pdfComCampo);
    }

    @Test
    void emExecucaoDentroDaValidade_saiSemAAssinatura() throws IOException {
        comSessoes(
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 2, 10, 0), "assinaturas/beneficiario.png"),
                sessao(SituacaoSessao.DISPONIVEL, null, null));

        assertThat(montagem.montar(guia)).isEqualTo(pdfComCampo);
    }

    @Test
    void comDuasAssinaturas_usaADaSessaoMaisRecente() throws IOException {
        comSessoes(
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 2, 10, 0), "assinaturas/antiga.png"),
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 9, 10, 0), "assinaturas/nova.png"));
        when(armazenamentoArquivo.carregar("assinaturas/nova.png"))
                .thenReturn(new ByteArrayResource(PdfsDeTeste.assinaturaPng()));

        montagem.montar(guia);

        verify(armazenamentoArquivo).carregar("assinaturas/nova.png");
        verify(armazenamentoArquivo, never()).carregar("assinaturas/antiga.png");
    }

    @Test
    void guiaFinalizadaSemORotulo_recusaOPdf() throws IOException {
        byte[] semCampo = PdfsDeTeste.pdfComRotulos(true, true, false);
        when(armazenamentoArquivo.carregar("guias/guia.pdf")).thenReturn(new ByteArrayResource(semCampo));
        comSessoes(
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 2, 10, 0), "assinaturas/beneficiario.png"));

        assertThatThrownBy(() -> montagem.montar(guia))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Assinatura do Respons");
    }

    @Test
    void guiaNaoFinalizadaSemORotulo_saiNormal() throws IOException {
        byte[] semCampo = PdfsDeTeste.pdfComRotulos(true, true, false);
        when(armazenamentoArquivo.carregar("guias/guia.pdf")).thenReturn(new ByteArrayResource(semCampo));
        comSessoes(sessao(SituacaoSessao.DISPONIVEL, null, null));

        assertThat(montagem.montar(guia)).isEqualTo(semCampo);
    }

    @Test
    void naoAlteraOArquivoGuardado() throws IOException {
        comSessoes(
                sessao(SituacaoSessao.ASSINADA, LocalDateTime.of(2026, 10, 2, 10, 0), "assinaturas/beneficiario.png"));

        montagem.montar(guia);

        verify(armazenamentoArquivo, never()).salvar(any(), any());
    }

    // ---------- fixtures ----------

    private void comSessoes(Sessao... sessoes) {
        when(sessaoRepository.findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                any())).thenReturn(List.of(sessoes));
    }

    private static Sessao sessao(SituacaoSessao status, LocalDateTime dataAssinatura, String imagem) {
        Sessao s = new Sessao();
        s.setStatus(status);
        s.setDataAssinatura(dataAssinatura);
        if (imagem != null) {
            Assinatura assinatura = new Assinatura();
            assinatura.setAssinaturaUrl(imagem);
            s.setAssinatura(assinatura);
        }
        return s;
    }

    /** Quantas imagens a primeira pagina do PDF declara como recurso (cada assinatura carimbada vira uma imagem). */
    private static int contarImagens(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDPage pagina = documento.getPage(0);
            PDResources recursos = pagina.getResources();
            int imagens = 0;
            if (recursos != null) {
                for (COSName nome : recursos.getXObjectNames()) {
                    imagens++;
                }
            }
            return imagens;
        }
    }
}
