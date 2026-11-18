package fatec.fusex.nexus.setorGuias;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;

@ExtendWith(MockitoExtension.class)
class AnexarGuiaServiceConferenciaPrecCpTest {

    private static final String EXAMES_OK =
            "[{\"codDgp\":\"10101160\",\"descricao\":\"Consulta ao oncologista\",\"quantidade\":1}]";
    // T16-BE2: UG e validade que passam na conferencia
    private static final String UG_OK = "Cmdo Bda Inf Amv";
    private static final String VALIDADE_OK = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"))
            .plusYears(1).toString();

    @Mock
    private AnexarGuiaRepository repository;

    @Mock
    private GuiaRascunhoProcedimentoRepository procedimentoRepository;

    @Mock
    private ArmazenamentoArquivo armazenamentoArquivo;

    @Mock
    private UsuarioLogado usuarioLogado;

    private AnexarGuiaService service;

    @BeforeEach
    void setUp() {
        service = new AnexarGuiaService(
                repository, procedimentoRepository, armazenamentoArquivo, usuarioLogado, 10_485_760L);
    }

    @Test
    void guiaDeOutroPaciente_recebe422SemSalvarNada() throws IOException {
        when(repository.buscarPreGuiaParaAnexar(1L)).thenReturn(Optional.of(preGuiaComPrecCp("45872213300")));
        MockMultipartFile pdf = pdfComTexto(
                "GUIA DE ENCAMINHAMENTO Nr: 202610280", "(PACIENTE)", "Prec CP: 99999999999");

        assertThrows(DadosInvalidosException.class, () -> service.anexar(1L, "202610280", pdf, EXAMES_OK, UG_OK, UG_OK, VALIDADE_OK));

        verify(armazenamentoArquivo, never()).salvar(any(), anyString());
    }

    @Test
    void precCpIgualComFormatacaoDiferente_grava() throws IOException {
        when(repository.buscarPreGuiaParaAnexar(1L)).thenReturn(Optional.of(preGuiaComPrecCp("458.722.133-00")));
        MockMultipartFile pdf = pdfComTexto(
                "GUIA DE ENCAMINHAMENTO Nr: 202610280", "(PACIENTE)", "Prec CP: 45872213300");

        service.anexar(1L, "202610280", pdf, EXAMES_OK, UG_OK, UG_OK, VALIDADE_OK);

        verify(armazenamentoArquivo).salvar(any(), anyString());
    }

    @Test
    void precCpNaoLido_grava() throws IOException {
        when(repository.buscarPreGuiaParaAnexar(1L)).thenReturn(Optional.of(preGuiaComPrecCp("45872213300")));
        MockMultipartFile pdf = pdfComTexto("GUIA DE ENCAMINHAMENTO Nr: 202610280", "Sem a marca do paciente");

        service.anexar(1L, "202610280", pdf, EXAMES_OK, UG_OK, UG_OK, VALIDADE_OK);

        verify(armazenamentoArquivo).salvar(any(), anyString());
    }

    @Test
    void preGuiaSemPrecCpCadastrado_grava() throws IOException {
        when(repository.buscarPreGuiaParaAnexar(1L)).thenReturn(Optional.of(preGuiaComPrecCp(null)));
        MockMultipartFile pdf = pdfComTexto(
                "GUIA DE ENCAMINHAMENTO Nr: 202610280", "(PACIENTE)", "Prec CP: 99999999999");

        service.anexar(1L, "202610280", pdf, EXAMES_OK, UG_OK, UG_OK, VALIDADE_OK);

        verify(armazenamentoArquivo).salvar(any(), anyString());
    }

    @Test
    void pdfIlegivel_naoBloqueiaEGrava() {
        when(repository.buscarPreGuiaParaAnexar(1L)).thenReturn(Optional.of(preGuiaComPrecCp("45872213300")));
        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "guia.pdf", "application/pdf", "%PDF-1.4 falso".getBytes());

        service.anexar(1L, "202610280", pdf, EXAMES_OK, UG_OK, UG_OK, VALIDADE_OK);

        verify(armazenamentoArquivo).salvar(any(), anyString());
    }

    private PreGuia preGuiaComPrecCp(String precCp) {
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setPrecCp(precCp);
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.EM_ANALISE);
        preGuia.setBeneficiario(beneficiario);
        return preGuia;
    }

    private static MockMultipartFile pdfComTexto(String... linhas) throws IOException {
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
            return new MockMultipartFile("pdf", "guia.pdf", "application/pdf", saida.toByteArray());
        }
    }
}