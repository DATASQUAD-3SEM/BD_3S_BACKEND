package fatec.fusex.nexus.guiaBeneficiario;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.SessaoDto;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * T18-BE1: regras de GET /guias/minhas.
 *
 * Ao listar, uma Guia em aberto cuja validade ja passou vira ENCERRADA (com ao menos uma sessao assinada)
 * ou VENCIDA (sem nenhuma), e as sessoes nao assinadas viram EXPIRADA. A virada grava no banco, entao cada
 * Guia e atualizada numa transacao propria, com lock; a leitura final roda em transacao somente leitura.
 */
@Service
public class GuiaBeneficiarioService {

    // Situacoes que ainda podem virar pela validade. REALIZADA, ENCERRADA e VENCIDA sao finais.
    private static final Set<SituacaoExecucao> EM_ABERTO =
            EnumSet.of(SituacaoExecucao.NAO_ENVIADA, SituacaoExecucao.ENVIADA, SituacaoExecucao.EM_EXECUCAO);

    private final UsuarioLogado usuarioLogado;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final TransactionTemplate transacaoEscrita;
    private final TransactionTemplate transacaoLeitura;

    public GuiaBeneficiarioService(UsuarioLogado usuarioLogado,
                                   GuiaRepository guiaRepository,
                                   GuiaProcedimentoRepository procedimentoRepository,
                                   SessaoRepository sessaoRepository,
                                   PlatformTransactionManager transactionManager) {
        this.usuarioLogado = usuarioLogado;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
    }

    public List<GuiaBeneficiarioResponse> listar() {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO);
        Long usuarioId = usuario.getIdUsuario();
        LocalDate hoje = Beneficiario.hoje();

        List<Long> candidatas = transacaoLeitura.execute(status ->
                guiaRepository.findByPreGuiaBeneficiarioUsuarioIdOrderByDataInsercaoDescIdGuiaDesc(usuarioId)
                        .stream()
                        .filter(guia -> precisaVirar(guia, hoje))
                        .map(Guia::getIdGuia)
                        .toList());

        for (Long idGuia : candidatas) {
            transacaoEscrita.executeWithoutResult(status -> virar(idGuia, hoje));
        }

        return transacaoLeitura.execute(status -> montar(usuarioId));
    }

    private boolean precisaVirar(Guia guia, LocalDate hoje) {
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

    private List<GuiaBeneficiarioResponse> montar(Long usuarioId) {
        List<Guia> guias = guiaRepository.findByPreGuiaBeneficiarioUsuarioIdOrderByDataInsercaoDescIdGuiaDesc(usuarioId);
        if (guias.isEmpty()) {
            return List.of();
        }
        List<Long> ids = guias.stream().map(Guia::getIdGuia).toList();

        Map<Long, List<ProcedimentoDto>> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        procedimento -> procedimento.getGuia().getIdGuia(),
                        Collectors.mapping(ProcedimentoDto::fromEntity, Collectors.toList())));

        Map<Long, List<SessaoDto>> sessoes = sessaoRepository
                .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        sessao -> sessao.getGuiaProcedimento().getGuia().getIdGuia(),
                        Collectors.mapping(SessaoDto::fromEntity, Collectors.toList())));

        return guias.stream()
                .map(guia -> GuiaBeneficiarioResponse.fromEntity(
                        guia,
                        procedimentos.getOrDefault(guia.getIdGuia(), List.of()),
                        sessoes.getOrDefault(guia.getIdGuia(), List.of())))
                .toList();
    }
}
