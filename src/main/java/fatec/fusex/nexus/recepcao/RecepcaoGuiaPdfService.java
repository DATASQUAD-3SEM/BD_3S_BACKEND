package fatec.fusex.nexus.recepcao;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class RecepcaoGuiaPdfService {

    private final GuiaRepository guiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    public RecepcaoGuiaPdfService(GuiaRepository guiaRepository,
                                  UsuarioRepository usuarioRepository,
                                  SimulacaoLogin simulacaoLogin,
                                  ArmazenamentoArquivo armazenamentoArquivo) {
        this.guiaRepository = guiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @Transactional(readOnly = true)
    public PdfGuiaRecepcao pdfDaGuia(Long guiaId, String modo) {
        exigirRecepcao();
        ModoPdfGuia modoEscolhido = ModoPdfGuia.de(modo);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + guiaId));

        if (guia.getPreGuia().getStatus() != PreGuiaStatus.CONCLUIDA) {
            throw new AcessoNegadoException(
                    "Guia indisponivel: a pre-guia ainda nao esta concluida com as duas assinaturas");
        }

        Resource arquivo = armazenamentoArquivo.carregar(guia.getArquivoUrl());
        return new PdfGuiaRecepcao(arquivo, "guia-" + guia.getNumero() + ".pdf", modoEscolhido.isAnexo());
    }

    private void exigirRecepcao() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.RECEPCAO) {
            throw new AcessoNegadoException("Acesso restrito a recepcao");
        }
    }
}