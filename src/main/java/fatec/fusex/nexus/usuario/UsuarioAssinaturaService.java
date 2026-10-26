package fatec.fusex.nexus.usuario;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;

/**
 * Grava a assinatura digital do usuario logado (T13-BE2) e remove o arquivo anterior ao substitui-la.
 * Setor de Guias e Recepcao nao possuem assinatura e recebem 403.
 */
@Service
public class UsuarioAssinaturaService {

    private static final String PASTA_ASSINATURAS = "assinaturas";

    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final AssinaturaImagemService assinaturaImagemService;

    public UsuarioAssinaturaService(
            UsuarioRepository usuarioRepository,
            SimulacaoLogin simulacaoLogin,
            ArmazenamentoArquivo armazenamentoArquivo,
            AssinaturaImagemService assinaturaImagemService) {

        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.assinaturaImagemService = assinaturaImagemService;
    }

    public void atualizarComArquivo(MultipartFile arquivo) {
        Usuario usuario = usuarioAutorizado();

        byte[] imagemNormalizada =
                assinaturaImagemService.normalizarUpload(arquivo);

        salvarNovaAssinatura(usuario, imagemNormalizada);
    }

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

    private void salvarNovaAssinatura(
            Usuario usuario, byte[] imagemNormalizada) {

        String caminhoAnterior = usuario.getAssinaturaUrl();

        MultipartFile arquivoNormalizado =
                new ArquivoAssinaturaMultipartFile(
                        imagemNormalizada,
                        "assinatura.png",
                        "image/png");

        String novoCaminho = armazenamentoArquivo.salvar(
                arquivoNormalizado, PASTA_ASSINATURAS);

        usuario.setAssinaturaUrl(novoCaminho);

        try {
            usuarioRepository.save(usuario);
        } catch (RuntimeException ex) {
            try {
                armazenamentoArquivo.remover(novoCaminho);
            } catch (RuntimeException erroAoLimpar) {
                ex.addSuppressed(erroAoLimpar);
            }

            throw ex;
        }

        if (caminhoAnterior != null
                && !caminhoAnterior.isBlank()
                && !caminhoAnterior.equals(novoCaminho)) {

            armazenamentoArquivo.remover(caminhoAnterior);
        }
    }
}