package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T30-BE5: Guia do menor finalizada com as sessoes assinadas pelo responsavel legal. O campo "Assinatura do
 * Responsavel" do PDF recebe a assinatura cadastrada do responsavel (a imagem gravada na sessao assinada).
 * O acesso ao PDF pelo responsavel e pelo dependente maior esta em GuiaPdfPacienteDependenteTest (T30-BE3).
 */
class MontagemPdfGuiaResponsavelTest {

    private static final String IMAGEM_RESPONSAVEL = "assinaturas/responsavel-legal.png";

    private SessaoRepository sessaoRepository;
    private ArmazenamentoArquivo armazenamentoArquivo;
    private MontagemPdfGuia montagem;
    private Guia guia;
    private Usuario responsavel;

    @BeforeEach
    void preparar() throws IOException {
        AssinaturaGuiaRepository assinaturaGuiaRepository = mock(AssinaturaGuiaRepository.class);
        sessaoRepository = mock(SessaoRepository.class);
        armazenamentoArquivo = mock(ArmazenamentoArquivo.class);
        montagem = new MontagemPdfGuia(assinaturaGuiaRepository, armazenamentoArquivo, new CarimboAssinaturaPdf(),
                sessaoRepository);

        guia = new Guia();
        guia.setIdGuia(1L);
        guia.setArquivoUrl("guias/guia-menor.pdf");
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
        when(armazenamentoArquivo.carregar("guias/guia-menor.pdf"))
                .thenReturn(new ByteArrayResource(PdfsDeTeste.pdfComRotulos(true, true, true)));
        when(armazenamentoArquivo.carregar(IMAGEM_RESPONSAVEL))
                .thenReturn(new ByteArrayResource(PdfsDeTeste.assinaturaPng()));
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(any())).thenReturn(List.of());

        responsavel = new Usuario();
        responsavel.setLogin("41282945067");
        responsavel.setPerfil(PerfilUsuario.RESPONSAVEL_EXTERNO);
    }

    @Test
    void guiaDoMenorFinalizada_levaAAssinaturaDoResponsavelLegal() throws IOException {
        when(sessaoRepository.findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                any())).thenReturn(List.of(
                        assinadaPeloResponsavel(LocalDateTime.of(2026, 10, 2, 10, 0)),
                        assinadaPeloResponsavel(LocalDateTime.of(2026, 10, 9, 10, 0))));

        byte[] pdf = montagem.montar(guia);

        verify(armazenamentoArquivo).carregar(IMAGEM_RESPONSAVEL);
        assertThat(contarImagens(pdf)).isEqualTo(1);
    }

    private Sessao assinadaPeloResponsavel(LocalDateTime quando) {
        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(responsavel);
        assinatura.setAssinaturaUrl(IMAGEM_RESPONSAVEL);

        Sessao sessao = new Sessao();
        sessao.setStatus(SituacaoSessao.ASSINADA);
        sessao.setDataAssinatura(quando);
        sessao.setAssinadoPor(responsavel);
        sessao.setAssinatura(assinatura);
        return sessao;
    }

    private static int contarImagens(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDResources recursos = documento.getPage(0).getResources();
            int imagens = 0;
            if (recursos != null) {
                for (COSName ignorado : recursos.getXObjectNames()) {
                    imagens++;
                }
            }
            return imagens;
        }
    }
}
