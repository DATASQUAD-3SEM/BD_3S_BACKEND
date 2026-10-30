package fatec.fusex.nexus.usuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinatura.AssinaturaRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;

/**
 * Cadastra uma assinatura digital para o usuario logado (T13-BE2). Cada cadastro cria uma linha nova
 * em Assinatura e troca apenas o ponteiro Usuario.assinatura. A assinatura anterior e o arquivo dela
 * sao mantidos, porque as Guias ja assinadas apontam para eles.
 * Setor de Guias e Recepcao nao possuem assinatura e recebem 403.
 */
@Service
public class UsuarioAssinaturaService {

    private static final String PASTA_ASSINATURAS = "assinaturas";

    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final AssinaturaImagemService assinaturaImagemService;
    private final AssinaturaRepository assinaturaRepository;

    public UsuarioAssinaturaService(
            UsuarioRepository usuarioRepository,
            SimulacaoLogin simulacaoLogin,
            ArmazenamentoArquivo armazenamentoArquivo,
            AssinaturaImagemService assinaturaImagemService,
            AssinaturaRepository assinaturaRepository) {

        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.assinaturaImagemService = assinaturaImagemService;
        this.assinaturaRepository = assinaturaRepository;
    }

    @Transactional
    public void atualizarComArquivo(MultipartFile arquivo) {
        Usuario usuario = usuarioAutorizado();

        byte[] imagemNormalizada =
                assinaturaImagemService.normalizarUpload(arquivo);

        salvarNovaAssinatura(usuario, imagemNormalizada);
    }

    @Transactional
    public void atualizarComBase64(String imagemBase64) {
        Usuario usuario = usuarioAutorizado();

        byte[] imagemNormalizada =
                assinaturaImagemService.normalizarBase64(imagemBase64);

        salvarNovaAssinatura(usuario, imagemNormalizada);
    }

    private Usuario usuarioAutorizado() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));

        PerfilUsuario perfil = usuario.getPerfil();

        boolean podeAssinar =
                perfil == PerfilUsuario.BENEFICIARIO
                        || perfil == PerfilUsuario.MEDICO
                        || perfil == PerfilUsuario.CHEFE_FUSEX;

        if (!podeAssinar) {
            throw new AcessoNegadoException(
                    "Este perfil nao tem permissao para cadastrar assinatura");
        }

        return usuario;
    }

    private void salvarNovaAssinatura(Usuario usuario, byte[] imagemNormalizada) {

        MultipartFile arquivoNormalizado =
                new ArquivoAssinaturaMultipartFile(
                        imagemNormalizada,
                        "assinatura.png",
                        "image/png");

        String novoCaminho = armazenamentoArquivo.salvar(
                arquivoNormalizado, PASTA_ASSINATURAS);

        try {
            Assinatura nova = new Assinatura();
            nova.setUsuario(usuario);
            nova.setAssinaturaUrl(novoCaminho);
            assinaturaRepository.save(nova);

            usuario.setAssinatura(nova);
            usuarioRepository.save(usuario);
        } catch (RuntimeException ex) {
            // Sem registro no banco, o arquivo recem-gravado nao tem para onde apontar: remove.
            try {
                armazenamentoArquivo.remover(novoCaminho);
            } catch (RuntimeException erroAoLimpar) {
                ex.addSuppressed(erroAoLimpar);
            }

            throw ex;
        }
    }
}