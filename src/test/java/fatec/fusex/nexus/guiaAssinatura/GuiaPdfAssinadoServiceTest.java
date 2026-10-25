package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.dto.PdfGuiaAssinada;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Testes da montagem do PDF da Guia com as assinaturas (T5-BE2): modos, Guia inexistente e perfil errado.
 */
class GuiaPdfAssinadoServiceTest {

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private AssinaturaGuiaRepository assinaturaGuiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private ArmazenamentoArquivo armazenamentoArquivo;

    @Mock
    private CarimboAssinaturaPdf carimboAssinaturaPdf;

    private GuiaPdfAssinadoService service;
    private byte[] pdfOriginal;

    @BeforeEach
    void configurar() throws Exception {
        MockitoAnnotations.openMocks(this);
        service = new GuiaPdfAssinadoService(guiaRepository, assinaturaGuiaRepository,
                new UsuarioLogado(simulacaoLogin, usuarioRepository), armazenamentoArquivo, carimboAssinaturaPdf);

        pdfOriginal = PdfsDeTeste.pdfComRotulos(true, true);

        when(simulacaoLogin.loginAtual()).thenReturn("medico");
        when(usuarioRepository.findByLogin("medico"))
                .thenReturn(Optional.of(usuario(PerfilUsuario.MEDICO, null)));
        when(guiaRepository.findById(1L)).thenReturn(Optional.of(guia()));
        when(armazenamentoArquivo.carregar("guias/guia.pdf"))
                .thenReturn(new ByteArrayResource(pdfOriginal));
    }

    @Test
    void semAssinaturas_devolveOPdfOriginal_eNaoCarimba() throws Exception {
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(1L))).thenReturn(List.of());

        PdfGuiaAssinada resultado = service.pdfDaGuia(1L, "visualizar");

        assertThat(resultado.arquivo().getInputStream().readAllBytes()).isEqualTo(pdfOriginal);
        verifyNoInteractions(carimboAssinaturaPdf);
    }

    @Test
    void comAssinatura_carimbaComAImagemDoPapelCorrespondente() throws Exception {
        byte[] png = PdfsDeTeste.assinaturaPng();
        Usuario medico = usuario(PerfilUsuario.MEDICO, "assinaturas/medico.png");
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(1L)))
                .thenReturn(List.of(assinatura(AssinaturaGuiaPapel.MEDICO, medico)));
        when(armazenamentoArquivo.carregar("assinaturas/medico.png"))
                .thenReturn(new ByteArrayResource(png));
        when(carimboAssinaturaPdf.aplicar(any(), anyMap())).thenReturn(new byte[] {1});

        service.pdfDaGuia(1L, "baixar");

        verify(carimboAssinaturaPdf).aplicar(any(), argThat(
                (Map<AssinaturaGuiaPapel, byte[]> mapa) ->
                        mapa.size() == 1 && mapa.containsKey(AssinaturaGuiaPapel.MEDICO)));
    }

    @Test
    void modoBaixar_marcaComoAnexo_eModoVisualizar_marcaComoInline() throws Exception {
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(1L))).thenReturn(List.of());

        assertThat(service.pdfDaGuia(1L, "baixar").anexo()).isTrue();
        assertThat(service.pdfDaGuia(1L, "visualizar").anexo()).isFalse();
    }

    @Test
    void nomeDoArquivo_usaONumeroDaGuia() {
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(List.of(1L))).thenReturn(List.of());

        assertThat(service.pdfDaGuia(1L, "visualizar").nomeArquivo())
                .isEqualTo("guia-202610678.pdf");
    }

    @Test
    void guiaInexistente_lancaRecursoNaoEncontrado() {
        when(guiaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pdfDaGuia(2L, "visualizar"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void perfilSemPermissao_lancaAcessoNegado() {
        when(usuarioRepository.findByLogin("medico"))
                .thenReturn(Optional.of(usuario(PerfilUsuario.RECEPCAO, null)));

        assertThatThrownBy(() -> service.pdfDaGuia(1L, "visualizar"))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void modoInvalido_lancaDadosInvalidos() {
        assertThatThrownBy(() -> service.pdfDaGuia(1L, "imprimir"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    private static Usuario usuario(PerfilUsuario perfil, String assinaturaUrl) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        usuario.setAssinaturaUrl(assinaturaUrl);
        return usuario;
    }

    private static AssinaturaGuia assinatura(AssinaturaGuiaPapel papel, Usuario usuario) {
        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setPapel(papel);
        assinatura.setUsuario(usuario);
        return assinatura;
    }

    private static Guia guia() {
        Guia guia = new Guia();
        guia.setIdGuia(1L);
        guia.setNumero("202610678");
        guia.setArquivoUrl("guias/guia.pdf");
        return guia;
    }
}