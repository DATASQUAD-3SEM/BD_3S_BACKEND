package fatec.fusex.nexus.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinatura.AssinaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;

/**
 * Testes da gravacao e substituicao da assinatura digital do usuario logado (T13-BE2)
 * e da leitura da imagem da assinatura atual (T37-BE3).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioAssinaturaServiceTest {

    private static final String LOGIN = "usuario-teste";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private ArmazenamentoArquivo armazenamentoArquivo;

    @Mock
    private AssinaturaRepository assinaturaRepository;

    private Usuario usuario;
    private UsuarioAssinaturaService service;

    @BeforeEach
    void configurar() {
        AssinaturaImagemService imagemService =
                new AssinaturaImagemService(
                        2 * 1024 * 1024L, 1200, 600);

        service = new UsuarioAssinaturaService(
                usuarioRepository,
                simulacaoLogin,
                armazenamentoArquivo,
                imagemService,
                assinaturaRepository);

        usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setLogin(LOGIN);
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(true);

        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    void atualizarComArquivo_quandoImagemValida_salvaAssinatura()
            throws IOException {

        when(armazenamentoArquivo.salvar(
                any(MultipartFile.class), eq("assinaturas")))
                .thenReturn("assinaturas/nova.png");

        service.atualizarComArquivo(arquivoPng());

        assertThat(usuario.getAssinatura().getAssinaturaUrl())
                .isEqualTo("assinaturas/nova.png");

        verify(usuarioRepository).save(usuario);
        verify(armazenamentoArquivo).salvar(
                any(MultipartFile.class), eq("assinaturas"));
    }

    @Test
    void atualizarComBase64_quandoImagemValida_salvaAssinatura()
            throws IOException {

        String base64 = Base64.getEncoder().encodeToString(
                criarImagemPng());

        when(armazenamentoArquivo.salvar(
                any(MultipartFile.class), eq("assinaturas")))
                .thenReturn("assinaturas/nova.png");

        service.atualizarComBase64(base64);

        assertThat(usuario.getAssinatura().getAssinaturaUrl())
                .isEqualTo("assinaturas/nova.png");

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void atualizarComArquivo_quandoImagemVazia_devolveErroDeValidacao() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "assinatura.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.atualizarComArquivo(arquivo))
                .isInstanceOf(DadosInvalidosException.class);

        verifyNoInteractions(armazenamentoArquivo);
    }

    @Test
    void atualizarComArquivo_quandoJaExisteAssinatura_naoRemoveArquivoAnterior()
            throws IOException {

        fatec.fusex.nexus.assinatura.Assinatura anterior =
                new fatec.fusex.nexus.assinatura.Assinatura();
        anterior.setAssinaturaUrl("assinaturas/anterior.png");
        usuario.setAssinatura(anterior);

        when(armazenamentoArquivo.salvar(
                any(MultipartFile.class), eq("assinaturas")))
                .thenReturn("assinaturas/nova.png");

        service.atualizarComArquivo(arquivoPng());

        assertThat(usuario.getAssinatura().getAssinaturaUrl())
                .isEqualTo("assinaturas/nova.png");
        verify(armazenamentoArquivo, never()).remover(anyString());
    }

    @ParameterizedTest
    @EnumSource(
            value = PerfilUsuario.class,
            names = {"BENEFICIARIO", "MEDICO", "CHEFE_FUSEX", "RESPONSAVEL_EXTERNO"})
    void atualizarComArquivo_quandoPerfilPermitido_aceitaAssinatura(
            PerfilUsuario perfil) throws IOException {

        usuario.setPerfil(perfil);

        service.atualizarComArquivo(arquivoPng());

        verify(usuarioRepository).save(usuario);
    }

    @ParameterizedTest
    @EnumSource(
            value = PerfilUsuario.class,
            names = {"SETOR_GUIAS", "RECEPCAO"})
    void atualizarComArquivo_quandoPerfilSemPermissao_devolve403(
            PerfilUsuario perfil) throws IOException {

        usuario.setPerfil(perfil);

        assertThatThrownBy(() -> service.atualizarComArquivo(arquivoPng()))
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(armazenamentoArquivo);
    }

    // ---------- T37-BE3: GET /usuarios/me/assinatura

    @Test
    void buscarImagemAtual_quandoDuasAssinaturasGravadas_devolveAMaisRecente()
            throws IOException {

        Map<String, byte[]> armazenados = simularArmazenamento(
                "assinaturas/primeira.png", "assinaturas/segunda.png");

        service.atualizarComArquivo(arquivoPng(criarImagemPng(190)));
        service.atualizarComArquivo(arquivoPng(criarImagemPng(60)));

        byte[] imagem = service.buscarImagemAtual();

        assertThat(imagem)
                .isEqualTo(armazenados.get("assinaturas/segunda.png"))
                .isNotEqualTo(armazenados.get("assinaturas/primeira.png"));
        verify(armazenamentoArquivo).carregar("assinaturas/segunda.png");
        verify(armazenamentoArquivo, never())
                .carregar("assinaturas/primeira.png");
    }

    @Test
    void buscarImagemAtual_quandoSemAssinatura_devolve404() {
        assertThatThrownBy(() -> service.buscarImagemAtual())
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Assinatura não cadastrada.");

        verifyNoInteractions(armazenamentoArquivo);
    }

    @ParameterizedTest
    @EnumSource(
            value = PerfilUsuario.class,
            names = {"SETOR_GUIAS", "RECEPCAO", "OCS", "SETOR_FATURA",
                    "RASTREABILIDADE", "AUDITORIA"})
    void buscarImagemAtual_quandoContaDeSetor_devolve403(
            PerfilUsuario perfil) {

        usuario.setPerfil(perfil);

        assertThatThrownBy(() -> service.buscarImagemAtual())
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(armazenamentoArquivo);
    }

    @ParameterizedTest
    @EnumSource(
            value = PerfilUsuario.class,
            names = {"BENEFICIARIO", "MEDICO", "CHEFE_FUSEX", "RESPONSAVEL_EXTERNO"})
    void buscarImagemAtual_quandoPerfilComAssinatura_devolveAImagem(
            PerfilUsuario perfil) {

        usuario.setPerfil(perfil);
        byte[] conteudo = {1, 2, 3};
        usuarioComAssinatura("assinaturas/atual.png");
        when(armazenamentoArquivo.carregar("assinaturas/atual.png"))
                .thenReturn(new ByteArrayResource(conteudo));

        assertThat(service.buscarImagemAtual()).isEqualTo(conteudo);
    }

    @Test
    void buscarImagemAtual_usaSoOUsuarioDoLogin() {
        byte[] conteudo = {9, 8, 7};
        usuarioComAssinatura("assinaturas/minha.png");
        when(armazenamentoArquivo.carregar("assinaturas/minha.png"))
                .thenReturn(new ByteArrayResource(conteudo));

        service.buscarImagemAtual();

        verify(usuarioRepository).findByLogin(LOGIN);
        verifyNoMoreInteractions(usuarioRepository);
    }

    @Test
    void buscarImagemAtual_depoisDeUmPut_devolveAImagemNovaComTamanhoFixo()
            throws IOException {

        simularArmazenamento("assinaturas/nova.png");

        String base64 = Base64.getEncoder().encodeToString(
                criarImagemPng(190));

        service.atualizarComBase64(base64);

        BufferedImage devolvida = ImageIO.read(
                new ByteArrayInputStream(service.buscarImagemAtual()));

        assertThat(devolvida.getWidth()).isEqualTo(900);
        assertThat(devolvida.getHeight()).isEqualTo(300);
    }

    private void usuarioComAssinatura(String caminho) {
        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(usuario);
        assinatura.setAssinaturaUrl(caminho);
        usuario.setAssinatura(assinatura);
    }

    /**
     * Armazenamento em memoria: salvar devolve os caminhos informados, na ordem, e carregar le de volta
     * os bytes gravados (o que o PUT guarda e o que o GET le).
     */
    private Map<String, byte[]> simularArmazenamento(String... caminhos) {
        Map<String, byte[]> armazenados = new HashMap<>();
        int[] proximo = {0};

        when(armazenamentoArquivo.salvar(
                any(MultipartFile.class), eq("assinaturas")))
                .thenAnswer(invocacao -> {
                    MultipartFile arquivo = invocacao.getArgument(0);
                    String caminho = caminhos[proximo[0]++];
                    armazenados.put(caminho, arquivo.getBytes());
                    return caminho;
                });

        when(armazenamentoArquivo.carregar(anyString()))
                .thenAnswer(invocacao -> new ByteArrayResource(
                        armazenados.get(invocacao.<String>getArgument(0))));

        return armazenados;
    }

    private MockMultipartFile arquivoPng(byte[] conteudo) {
        return new MockMultipartFile(
                "arquivo", "assinatura.png", "image/png", conteudo);
    }

    private MockMultipartFile arquivoPng() throws IOException {
        return new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                "image/png",
                criarImagemPng());
    }

    private byte[] criarImagemPng() throws IOException {
        return criarImagemPng(190);
    }

    private byte[] criarImagemPng(int fimDoTraco) throws IOException {
        BufferedImage imagem = new BufferedImage(
                200, 80, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graficos = imagem.createGraphics();

        try {
            graficos.setColor(Color.BLACK);
            graficos.drawLine(10, 40, 100, 20);
            graficos.drawLine(100, 20, fimDoTraco, 40);
        } finally {
            graficos.dispose();
        }

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);

        return saida.toByteArray();
    }
}