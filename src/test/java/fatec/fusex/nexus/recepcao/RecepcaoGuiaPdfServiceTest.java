package fatec.fusex.nexus.recepcao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

class RecepcaoGuiaPdfServiceTest {

    private static final byte[] CONTEUDO = "%PDF-1.4 conteudo".getBytes();
    private static final String CAMINHO = "guias/00042.pdf";

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private ArmazenamentoArquivo armazenamentoArquivo;

    private AutoCloseable mocks;

    private RecepcaoGuiaPdfService service;

    @BeforeEach
    void configurar() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new RecepcaoGuiaPdfService(guiaRepository, new UsuarioLogado(simulacaoLogin, usuarioRepository), armazenamentoArquivo);

        when(simulacaoLogin.loginAtual()).thenReturn("recepcao");
        when(usuarioRepository.findByLogin("recepcao")).thenReturn(Optional.of(usuario(PerfilUsuario.RECEPCAO)));
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia(PreGuiaStatus.CONCLUIDA)));
        when(armazenamentoArquivo.carregar(CAMINHO)).thenReturn(new ByteArrayResource(CONTEUDO));
    }

    @AfterEach
    void fecharMocks() throws Exception {
        mocks.close();
    }

    @Test
    void pdf_visualizar_devolveInlineComNomeDaGuia() throws IOException {
        PdfGuiaRecepcao pdf = service.pdfDaGuia(1L, "visualizar");

        assertThat(pdf.anexo()).isFalse();
        assertThat(pdf.nomeArquivo()).isEqualTo("guia-00042.pdf");
        assertThat(pdf.arquivo().getContentAsByteArray()).isEqualTo(CONTEUDO);
    }

    @Test
    void pdf_imprimir_devolveInline() {
        PdfGuiaRecepcao pdf = service.pdfDaGuia(1L, "imprimir");

        assertThat(pdf.anexo()).isFalse();
    }

    @Test
    void pdf_baixar_devolveAttachment() {
        PdfGuiaRecepcao pdf = service.pdfDaGuia(1L, "baixar");

        assertThat(pdf.anexo()).isTrue();
    }

    @Test
    void pdf_guiaDePreGuiaNaoConcluida_lancaAcessoNegado_enaoLeOArquivo() {
        for (PreGuiaStatus status : List.of(PreGuiaStatus.PENDENTE, PreGuiaStatus.EM_ANALISE, PreGuiaStatus.ATENCAO)) {
            when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia(status)));

            assertThatThrownBy(() -> service.pdfDaGuia(1L, "visualizar"))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        verifyNoInteractions(armazenamentoArquivo);
    }

    @Test
    void pdf_guiaInexistente_lancaRecursoNaoEncontrado() {
        when(guiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pdfDaGuia(99L, "visualizar"))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(armazenamentoArquivo);
    }

    @Test
    void pdf_perfilQueNaoEhRecepcao_lancaAcessoNegado_antesDeTocarEmNada() {
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            if (perfil == PerfilUsuario.RECEPCAO) {
                continue;
            }
            when(usuarioRepository.findByLogin("recepcao")).thenReturn(Optional.of(usuario(perfil)));

            assertThatThrownBy(() -> service.pdfDaGuia(1L, "visualizar"))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        verifyNoInteractions(guiaRepository, armazenamentoArquivo);
    }

    @Test
    void pdf_usuarioLogadoInexistente_lancaNaoAutenticado() {
        when(usuarioRepository.findByLogin("recepcao")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pdfDaGuia(1L, "visualizar"))
                .isInstanceOf(NaoAutenticadoException.class);

        verifyNoInteractions(guiaRepository, armazenamentoArquivo);
    }

    @Test
    void pdf_modoAusenteOuInvalido_lancaRegraDeNegocio_enaoConsultaAGuia() {
        assertThatThrownBy(() -> service.pdfDaGuia(1L, null)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.pdfDaGuia(1L, "")).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.pdfDaGuia(1L, "pdf")).isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(armazenamentoArquivo);
    }

    @Test
    void pdf_arquivoSumiuDoArmazenamento_propagaRecursoNaoEncontrado() {
        when(armazenamentoArquivo.carregar(CAMINHO))
                .thenThrow(new RecursoNaoEncontradoException("Arquivo nao encontrado no armazenamento"));

        assertThatThrownBy(() -> service.pdfDaGuia(1L, "visualizar"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void modoPdfGuia_aceitaCaixaEEspacos() {
        assertThat(ModoPdfGuia.de("BAIXAR")).isEqualTo(ModoPdfGuia.BAIXAR);
        assertThat(ModoPdfGuia.de(" imprimir ")).isEqualTo(ModoPdfGuia.IMPRIMIR);
        assertThat(ModoPdfGuia.de("Visualizar")).isEqualTo(ModoPdfGuia.VISUALIZAR);
    }


    private static Usuario usuario(PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin("recepcao");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static Guia guia(PreGuiaStatus status) {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(status);

        Guia guia = new Guia();
        guia.setIdGuia(1L);
        guia.setNumero("00042");
        guia.setArquivoUrl(CAMINHO);
        guia.setPreGuia(preGuia);
        return guia;
    }
}