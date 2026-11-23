package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.PlatformTransactionManager;

import fatec.fusex.nexus.common.rotina.RotinaExecucao;
import fatec.fusex.nexus.common.rotina.RotinaExecucaoRepository;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;

// T16-BE3: correcao unica das Guias antigas a partir do PDF guardado (sem contexto Spring)
@ExtendWith(MockitoExtension.class)
class CorrecaoValidadeGuiasTest {

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private ArmazenamentoArquivo armazenamentoArquivo;

    @Mock
    private RotinaExecucaoRepository rotinaExecucaoRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private CorrecaoValidadeGuias correcao;

    @BeforeEach
    void setUp() {
        correcao = new CorrecaoValidadeGuias(guiaRepository, armazenamentoArquivo, rotinaExecucaoRepository,
                transactionManager);
    }

    @Test
    void guiaSemValidade_releOPdfEGravaADataDeNaoReceberApos() throws IOException {
        Guia guia = guia(1L, "202610678", "guias-rascunho/202610678.pdf");
        when(rotinaExecucaoRepository.existsById(CorrecaoValidadeGuias.NOME_ROTINA)).thenReturn(false);
        when(guiaRepository.findByDataValidadeAteIsNull()).thenReturn(List.of(guia));
        when(armazenamentoArquivo.carregar("guias-rascunho/202610678.pdf"))
                .thenReturn(new ByteArrayResource(pdfModelo()));
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia));

        Optional<CorrecaoValidadeGuias.Resultado> resultado = correcao.executar();

        assertThat(resultado).contains(new CorrecaoValidadeGuias.Resultado(1, 0));
        // guia-modelo.pdf traz "NAO RECEBER APOS: 20/10/2026"
        assertThat(guia.getDataValidadeAte()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void pdfSemDataOuArquivoAusente_ficaSemValidadeELogadoSemDerrubarAsOutras() throws IOException {
        Guia semData = guia(2L, "202610001", "guias-rascunho/semdata.pdf");
        Guia ausente = guia(3L, "202610002", "guias-rascunho/ausente.pdf");
        when(rotinaExecucaoRepository.existsById(CorrecaoValidadeGuias.NOME_ROTINA)).thenReturn(false);
        when(guiaRepository.findByDataValidadeAteIsNull()).thenReturn(List.of(semData, ausente));
        when(armazenamentoArquivo.carregar("guias-rascunho/semdata.pdf"))
                .thenReturn(new ByteArrayResource(pdfVazio()));
        when(armazenamentoArquivo.carregar("guias-rascunho/ausente.pdf"))
                .thenThrow(new IllegalStateException("arquivo nao encontrado"));

        Optional<CorrecaoValidadeGuias.Resultado> resultado = correcao.executar();

        assertThat(resultado).contains(new CorrecaoValidadeGuias.Resultado(0, 2));
        assertThat(semData.getDataValidadeAte()).isNull();
        assertThat(ausente.getDataValidadeAte()).isNull();
        verify(guiaRepository, never()).findById(anyLong());
    }

    @Test
    void semGuiasSemValidade_nadaAconteceMasARotinaFicaRegistrada() {
        when(rotinaExecucaoRepository.existsById(CorrecaoValidadeGuias.NOME_ROTINA)).thenReturn(false);
        when(guiaRepository.findByDataValidadeAteIsNull()).thenReturn(List.of());

        assertThat(correcao.executar()).contains(new CorrecaoValidadeGuias.Resultado(0, 0));
        verify(armazenamentoArquivo, never()).carregar(anyString());
        verify(rotinaExecucaoRepository).save(any(RotinaExecucao.class));
    }

    @Test
    void rotinaJaExecutada_naoLeNemAlteraNada() {
        when(rotinaExecucaoRepository.existsById(CorrecaoValidadeGuias.NOME_ROTINA)).thenReturn(true);

        assertThat(correcao.executar()).isEmpty();
        verify(guiaRepository, never()).findByDataValidadeAteIsNull();
        verify(rotinaExecucaoRepository, never()).save(any(RotinaExecucao.class));
    }

    private static Guia guia(Long id, String numero, String arquivoUrl) {
        Guia guia = new Guia();
        guia.setIdGuia(id);
        guia.setNumero(numero);
        guia.setArquivoUrl(arquivoUrl);
        return guia;
    }

    private static byte[] pdfModelo() throws IOException {
        try (var entrada = CorrecaoValidadeGuiasTest.class.getResourceAsStream("/guia-modelo.pdf")) {
            return entrada.readAllBytes();
        }
    }

    // PDF valido sem nenhum texto: nao traz NAO RECEBER APOS
    private static byte[] pdfVazio() throws IOException {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            documento.addPage(new PDPage());
            documento.save(saida);
            return saida.toByteArray();
        }
    }
}
