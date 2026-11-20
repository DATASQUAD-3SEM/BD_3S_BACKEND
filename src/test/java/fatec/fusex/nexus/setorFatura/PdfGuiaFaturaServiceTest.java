package fatec.fusex.nexus.setorFatura;

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

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.espelho.StatusEspelho;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.CarimboAssinaturaPdf;
import fatec.fusex.nexus.guiaAssinatura.MontagemPdfGuia;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

class PdfGuiaFaturaServiceTest {

    private static final byte[] CONTEUDO = "%PDF-1.4 conteudo".getBytes();
    private static final String CAMINHO = "guias/00042.pdf";
    private static final String LOGIN = "setor.lisura";

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private ArmazenamentoArquivo armazenamentoArquivo;

    @Mock
    private AssinaturaGuiaRepository assinaturaGuiaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    private AutoCloseable mocks;

    private PdfGuiaFaturaService service;

    @BeforeEach
    void configurar() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new PdfGuiaFaturaService(
                new UsuarioLogado(simulacaoLogin, usuarioRepository),
                guiaRepository,
                new MontagemPdfGuia(assinaturaGuiaRepository, armazenamentoArquivo, new CarimboAssinaturaPdf(),
                        sessaoRepository));

        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(armazenamentoArquivo.carregar(CAMINHO)).thenReturn(new ByteArrayResource(CONTEUDO));
    }

    @AfterEach
    void fecharMocks() throws Exception {
        mocks.close();
    }

    @Test
    void guiaDeFaturaEmLisuraOuDepois_devolvePdfComNomeDaGuia() throws IOException {
        for (SituacaoFatura situacao : List.of(SituacaoFatura.EM_LISURA, SituacaoFatura.AGUARDANDO_OCS,
                SituacaoFatura.EM_AUDITORIA, SituacaoFatura.AUDITADA)) {
            when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario(PerfilUsuario.RASTREABILIDADE)));
            when(guiaRepository.findById(1L)).thenReturn(Optional.of(guiaDaFatura(situacao)));

            PdfGuiaFatura pdf = service.pdfDaGuia(1L, PerfilUsuario.RASTREABILIDADE);

            assertThat(pdf.nomeArquivo()).isEqualTo("guia-00042.pdf");
            assertThat(pdf.arquivo().getContentAsByteArray()).isEqualTo(CONTEUDO);
        }
    }

    @Test
    void perfilAuditoriaAbreOPdfQuandoAFaturaEstaEmAuditoria() throws IOException {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario(PerfilUsuario.AUDITORIA)));
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guiaDaFatura(SituacaoFatura.EM_AUDITORIA)));

        PdfGuiaFatura pdf = service.pdfDaGuia(1L, PerfilUsuario.AUDITORIA);

        assertThat(pdf.arquivo().getContentAsByteArray()).isEqualTo(CONTEUDO);
    }

    @Test
    void guiaDeFaturaSoProtocoladaOuDeEspelhoEmConstrucaoOuSemEspelho_devolve404() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario(PerfilUsuario.RASTREABILIDADE)));

        for (Guia guia : List.of(
                guiaDaFatura(SituacaoFatura.RECEBIDO),
                guiaDaFatura(SituacaoFatura.PROTOCOLADA),
                guiaEmConstrucao(),
                guiaSemEspelho())) {
            when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia));

            assertThatThrownBy(() -> service.pdfDaGuia(1L, PerfilUsuario.RASTREABILIDADE))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        verifyNoInteractions(armazenamentoArquivo);
    }

    @Test
    void guiaInexistente_devolve404() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario(PerfilUsuario.RASTREABILIDADE)));
        when(guiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pdfDaGuia(99L, PerfilUsuario.RASTREABILIDADE))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(armazenamentoArquivo);
    }

    @Test
    void perfilQueNaoEhODaRota_lancaAcessoNegado_antesDeConsultarAGuia() {
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            if (perfil == PerfilUsuario.RASTREABILIDADE) {
                continue;
            }
            when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario(perfil)));

            assertThatThrownBy(() -> service.pdfDaGuia(1L, PerfilUsuario.RASTREABILIDADE))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        verifyNoInteractions(guiaRepository, armazenamentoArquivo);
    }

    @Test
    void usuarioLogadoInexistente_lancaNaoAutenticado() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pdfDaGuia(1L, PerfilUsuario.RASTREABILIDADE))
                .isInstanceOf(NaoAutenticadoException.class);

        verifyNoInteractions(guiaRepository, armazenamentoArquivo);
    }

    private static Usuario usuario(PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(LOGIN);
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static Guia guiaDaFatura(SituacaoFatura situacao) {
        Espelho espelho = new Espelho();
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setSituacaoFatura(situacao);

        Guia guia = guiaBase();
        guia.setEspelho(espelho);
        return guia;
    }

    private static Guia guiaEmConstrucao() {
        Espelho espelho = new Espelho();
        espelho.setStatus(StatusEspelho.EM_CONSTRUCAO);

        Guia guia = guiaBase();
        guia.setEspelho(espelho);
        return guia;
    }

    private static Guia guiaSemEspelho() {
        return guiaBase();
    }

    private static Guia guiaBase() {
        Guia guia = new Guia();
        guia.setIdGuia(1L);
        guia.setNumero("00042");
        guia.setArquivoUrl(CAMINHO);
        guia.setPreGuia(new PreGuia());
        return guia;
    }
}