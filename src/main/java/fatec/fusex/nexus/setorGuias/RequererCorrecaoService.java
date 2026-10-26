package fatec.fusex.nexus.setorGuias;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.setorGuias.dto.CorrecaoRequeridaResponse;
import fatec.fusex.nexus.setorGuias.dto.RequererCorrecaoRequest;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

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
    private final UsuarioLogado usuarioLogado;

    public RequererCorrecaoService(RequererCorrecaoRepository preGuiaRepository,
                                   PreGuiaHistoricoRepository historicoRepository,
                                   UsuarioLogado usuarioLogado) {
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Ordem: perfil (403) -> existe (404) -> observacao (422) -> status (409)
    @Transactional
    public CorrecaoRequeridaResponse requerer(Long id, RequererCorrecaoRequest request) {
        Usuario setor = usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

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
}
