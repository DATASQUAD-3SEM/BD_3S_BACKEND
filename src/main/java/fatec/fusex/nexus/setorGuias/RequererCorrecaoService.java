package fatec.fusex.nexus.setorGuias;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.setorGuias.dto.CorrecaoRequeridaResponse;
import fatec.fusex.nexus.setorGuias.dto.RequererCorrecaoRequest;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T4-BE4: POST /setor-guias/pre-guias/{id}/correcao.
 * A pre-guia em Em analise vira Atencao e o historico guarda a observacao, o ator SETOR_GUIAS e a data/hora.
 * Essa data aparece para o beneficiario (e para o medico) como "Registrada em".
 */
@Service
public class RequererCorrecaoService {

    private static final int OBSERVACAO_MAXIMO = 500; // tamanho da coluna PreGuiaHistorico.observacao

    private final RequererCorrecaoRepository preGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public RequererCorrecaoService(RequererCorrecaoRepository preGuiaRepository,
                                   PreGuiaHistoricoRepository historicoRepository,
                                   UsuarioRepository usuarioRepository,
                                   SimulacaoLogin simulacaoLogin) {
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    // Ordem: perfil (403) -> existe (404) -> observacao (422) -> status (409)
    @Transactional
    public CorrecaoRequeridaResponse requerer(Long id, RequererCorrecaoRequest request) {
        Usuario setor = validarSetorGuias();

        PreGuia preGuia = preGuiaRepository.buscarParaCorrigir(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + id));

        String observacao = validarObservacao(request == null ? null : request.observacao());

        if (preGuia.getStatus() != PreGuiaStatus.EM_ANALISE) {
            throw new ConflitoException("A pre-guia nao esta Em analise: ela ja foi alterada");
        }

        LocalDateTime dataDecisao = LocalDateTime.now();
        preGuia.setStatus(PreGuiaStatus.ATENCAO);
        historicoRepository.save(new PreGuiaHistorico(
                null, preGuia, PreGuiaStatus.ATENCAO, observacao, null, setor, dataDecisao));

        return new CorrecaoRequeridaResponse(preGuia.getStatus().name(), dataDecisao);
    }

    // Obrigatoria, sem apenas espacos e com no maximo 500 caracteres
    static String validarObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            throw new DadosInvalidosException("observacao", "Informe a observacao");
        }
        String limpa = observacao.trim();
        if (limpa.length() > OBSERVACAO_MAXIMO) {
            throw new DadosInvalidosException("observacao",
                    "A observacao deve ter no maximo " + OBSERVACAO_MAXIMO + " caracteres");
        }
        return limpa;
    }

    private Usuario validarSetorGuias() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.SETOR_GUIAS) {
            throw new AcessoNegadoException("Acesso negado: este endpoint e exclusivo do SETOR_GUIAS");
        }
        return usuario;
    }
}
