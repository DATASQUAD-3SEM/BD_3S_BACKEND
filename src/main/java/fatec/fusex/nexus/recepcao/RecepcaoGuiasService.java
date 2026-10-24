package fatec.fusex.nexus.recepcao;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.recepcao.dto.GuiaRecepcaoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class RecepcaoGuiasService {

    static final int MINIMO_CARACTERES = 3;
    static final int LIMITE_RESULTADOS = 50;

    private final GuiaRepository guiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public RecepcaoGuiasService(GuiaRepository guiaRepository,
                                UsuarioRepository usuarioRepository,
                                SimulacaoLogin simulacaoLogin) {
        this.guiaRepository = guiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    // T6-BE1: busca por nome do beneficiario, Prec CP ou numero da Guia (so Guias concluidas)
    @Transactional(readOnly = true)
    public List<GuiaRecepcaoResponse> buscar(String busca) {
        // O perfil e checado ANTES de validar o termo: quem nao e da recepcao nao descobre regras de busca.
        exigirRecepcao();

        String termo = busca == null ? "" : busca.trim();
        if (termo.length() < MINIMO_CARACTERES) {
            throw new RegraDeNegocioException("Informe ao menos 3 caracteres para buscar");
        }

        return guiaRepository
                .buscarConcluidasParaRecepcao(PreGuiaStatus.CONCLUIDA, termo, extrairPrecCp(termo),
                        PageRequest.of(0, LIMITE_RESULTADOS))
                .stream()
                .map(GuiaRecepcaoResponse::de)
                .toList();
    }

    /** Aceita o Prec CP com ou sem mascara. Devolve null se o termo nao for so digitos. */
    static String extrairPrecCp(String termo) {
        String digitos = termo.replace(".", "").replace("-", "").replace(" ", "");
        return digitos.matches("[0-9]+") ? digitos : null;
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