package fatec.fusex.nexus.guia;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;

/**
 * Virada de validade da Guia (T18-BE1, reaproveitada na T19-BE1).
 *
 * Uma Guia em aberto cuja validade ja passou vira ENCERRADA (com ao menos uma sessao assinada) ou VENCIDA
 * (sem nenhuma), e as sessoes nao assinadas viram EXPIRADA. A virada grava no banco, entao cada Guia e
 * atualizada numa transacao propria, relendo a Guia com lock.
 */
@Component
public class ViradaValidadeGuia {

    // Situacoes que ainda podem virar pela validade. REALIZADA, ENCERRADA e VENCIDA sao finais.
    static final Set<SituacaoExecucao> EM_ABERTO =
            EnumSet.of(SituacaoExecucao.NAO_ENVIADA, SituacaoExecucao.ENVIADA, SituacaoExecucao.EM_EXECUCAO);

    private final GuiaRepository guiaRepository;
    private final SessaoRepository sessaoRepository;
    private final TransactionTemplate transacaoEscrita;

    public ViradaValidadeGuia(GuiaRepository guiaRepository,
                              SessaoRepository sessaoRepository,
                              PlatformTransactionManager transactionManager) {
        this.guiaRepository = guiaRepository;
        this.sessaoRepository = sessaoRepository;
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
    }

    /** Aplica a virada nas Guias informadas que ja venceram. Cada uma em transacao propria. */
    public void virarVencidas(Collection<Guia> guias, LocalDate hoje) {
        List<Long> candidatas = guias.stream()
                .filter(guia -> precisaVirar(guia, hoje))
                .map(Guia::getIdGuia)
                .toList();

        for (Long idGuia : candidatas) {
            transacaoEscrita.executeWithoutResult(status -> virar(idGuia, hoje));
        }
    }

    public static boolean precisaVirar(Guia guia, LocalDate hoje) {
        return guia.getDataValidadeAte() != null
                && hoje.isAfter(guia.getDataValidadeAte())
                && EM_ABERTO.contains(guia.getSituacaoExecucao());
    }

    // Relê com lock: outra requisicao pode ter mudado a Guia depois da leitura da lista.
    private void virar(Long idGuia, LocalDate hoje) {
        Guia guia = guiaRepository.findByIdComLock(idGuia).orElse(null);
        if (guia == null || !precisaVirar(guia, hoje)) {
            return;
        }

        List<Sessao> sessoes = sessaoRepository.buscarComLockPorGuia(idGuia);
        sessoes.stream()
                .filter(sessao -> sessao.getStatus() != SituacaoSessao.ASSINADA)
                .forEach(sessao -> sessao.setStatus(SituacaoSessao.EXPIRADA));

        boolean algumaAssinada = sessoes.stream()
                .anyMatch(sessao -> sessao.getStatus() == SituacaoSessao.ASSINADA);
        guia.setSituacaoExecucao(algumaAssinada ? SituacaoExecucao.ENCERRADA : SituacaoExecucao.VENCIDA);
    }
}
