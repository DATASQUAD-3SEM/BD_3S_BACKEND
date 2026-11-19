package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import fatec.fusex.nexus.common.exception.LimiteRequisicoesException;

/** Regras puras do T11-BE1: extracao do numero no texto do PDF e limite de leituras por minuto.
 *  Estendido pela T16-BE1: extracao de UG, UG Origem e Validade do PDF. */
class ExtrairNumeroGuiaServiceTest {

    @Test
    void extrai_o_numero_depois_do_cabecalho() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("GUIA DE ENCAMINHAMENTO\nNr: 202610678"))
                .contains("202610678");
    }

    @Test
    void aceita_espacos_e_caixa_diferente_no_cabecalho() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("Guia de  Encaminhamento\nNr:202610678"))
                .contains("202610678");
    }

    @Test
    void sem_o_cabecalho_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("Nr: 202610678")).isEmpty();
    }

    @Test
    void cabecalho_sem_o_padrao_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("GUIA DE ENCAMINHAMENTO\nSem numero")).isEmpty();
    }

    @Test
    void numero_com_mais_de_20_digitos_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto(
                "GUIA DE ENCAMINHAMENTO Nr: 123456789012345678901")).isEmpty();
    }

    @Test
    void texto_vazio_ou_nulo_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto(null)).isEmpty();
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("   ")).isEmpty();
    }

    @Test
    void limite_de_leituras_bloqueia_a_21a_e_libera_na_janela_seguinte() {
        RelogioManual relogio = new RelogioManual(Instant.parse("2026-11-05T12:00:00Z"));
        LimiteLeituraGuia limite = new LimiteLeituraGuia(relogio);

        for (int i = 0; i < 20; i++) {
            limite.registrar("setor.teste");
        }
        assertThatThrownBy(() -> limite.registrar("setor.teste"))
                .isInstanceOf(LimiteRequisicoesException.class);

        relogio.avancar(Duration.ofMinutes(1));
        limite.registrar("setor.teste");
    }

    // ==========================================
    // T16-BE1: Testes de extração de UG, UG Origem e Validade
    // ==========================================

    @Test
    void extrai_ug_ug_origem_e_validade_do_pdf_modelo() throws IOException {
        MockMultipartFile pdf = gerarPdf(
                "GUIA DE ENCAMINHAMENTO",
                "Nr: 202610678",
                "UG: Cmdo Bda Inf Amv",
                "UG Origem: Cmdo Bda Inf Amv",
                "NÃO RECEBER APÓS: 20/10/2026"
        );
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        assertThat(LeitorGuiaPdf.extrairUg(texto)).contains("Cmdo Bda Inf Amv");
        assertThat(LeitorGuiaPdf.extrairUgOrigem(texto)).contains("Cmdo Bda Inf Amv");
        assertThat(LeitorGuiaPdf.extrairValidade(texto)).contains("2026-10-20");
    }

    @Test
    void extrai_ug_origem_quebrada_em_duas_linhas() throws IOException {
        MockMultipartFile pdf = gerarPdf(
                "GUIA DE ENCAMINHAMENTO",
                "Nr: 202610678",
                "UG: Cmdo Bda Inf Amv",
                "UG Origem: Cmdo Bda Inf",
                "Amv",
                "NÃO RECEBER APÓS: 20/10/2026"
        );
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        assertThat(LeitorGuiaPdf.extrairUgOrigem(texto)).contains("Cmdo Bda Inf Amv");
    }

    @Test
    void aceita_caixa_e_acento_variados_na_validade() throws IOException {
        MockMultipartFile pdf = gerarPdf(
                "GUIA DE ENCAMINHAMENTO",
                "Nr: 202610678",
                "UG: Cmdo Bda Inf Amv",
                "UG Origem: Cmdo Bda Inf Amv",
                "nao receber apos: 20/10/2026"
        );
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        assertThat(LeitorGuiaPdf.extrairValidade(texto)).contains("2026-10-20");
    }

    @Test
    void data_invalida_retorna_vazio() throws IOException {
        MockMultipartFile pdf = gerarPdf(
                "GUIA DE ENCAMINHAMENTO",
                "Nr: 202610678",
                "UG: Cmdo Bda Inf Amv",
                "UG Origem: Cmdo Bda Inf Amv",
                "NÃO RECEBER APÓS: 31/02/2027"
        );
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        assertThat(LeitorGuiaPdf.extrairValidade(texto)).isEmpty();
    }

    @Test
    void ug_ausente_retorna_vazio() throws IOException {
        MockMultipartFile pdf = gerarPdf(
                "GUIA DE ENCAMINHAMENTO",
                "Nr: 202610678",
                "UG Origem: Cmdo Bda Inf Amv",
                "NÃO RECEBER APÓS: 20/10/2026"
        );
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        assertThat(LeitorGuiaPdf.extrairUg(texto)).isEmpty();
    }

    @Test
    void ug_origem_ausente_retorna_vazio() throws IOException {
        MockMultipartFile pdf = gerarPdf(
                "GUIA DE ENCAMINHAMENTO",
                "Nr: 202610678",
                "UG: Cmdo Bda Inf Amv",
                "NÃO RECEBER APÓS: 20/10/2026"
        );
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        assertThat(LeitorGuiaPdf.extrairUgOrigem(texto)).isEmpty();
    }

    @Test
    void validade_ausente_retorna_vazio() throws IOException {
        MockMultipartFile pdf = gerarPdf(
                "GUIA DE ENCAMINHAMENTO",
                "Nr: 202610678",
                "UG: Cmdo Bda Inf Amv",
                "UG Origem: Cmdo Bda Inf Amv"
        );
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        assertThat(LeitorGuiaPdf.extrairValidade(texto)).isEmpty();
    }

    private MockMultipartFile gerarPdf(String... linhas) throws IOException {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                conteudo.beginText();
                conteudo.newLineAtOffset(50, 750);
                for (String linha : linhas) {
                    conteudo.showText(linha);
                    conteudo.newLineAtOffset(0, -14);
                }
                conteudo.endText();
            }
            documento.save(saida);
            return new MockMultipartFile("pdf", "guia-modelo.pdf", "application/pdf", saida.toByteArray());
        }
    }

    /** Relogio que o teste avanca na mao, para cobrir a janela de um minuto sem esperar. */
    private static final class RelogioManual extends Clock {

        private Instant agora;

        RelogioManual(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}