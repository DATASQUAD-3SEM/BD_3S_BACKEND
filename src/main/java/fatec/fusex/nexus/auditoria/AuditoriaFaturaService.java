package fatec.fusex.nexus.auditoria;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.andamentoGlosa.AndamentoGlosaService;
import fatec.fusex.nexus.auditoria.dto.AuditoriaDetalheResponse;
import fatec.fusex.nexus.auditoria.dto.AuditoriaResumoResponse;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.rastreabilidade.dto.EnviarFaturaRequest;
import fatec.fusex.nexus.setorFatura.FaturaConsultaService;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/** Auditoria (T26-BE1..BE3): so visualiza a fatura e conclui o fluxo; nao altera valores. */
@Service
public class AuditoriaFaturaService {

    private static final Set<SituacaoFatura> VISIVEIS_NO_DETALHE =
            EnumSet.of(SituacaoFatura.EM_AUDITORIA, SituacaoFatura.AUDITADA);

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final FaturaEventoRepository eventoRepository;
    private final FaturaTransicaoService transicaoService;
    private final FaturaConsultaService consultaService;
    private final AndamentoGlosaService andamentoGlosaService;

    public AuditoriaFaturaService(UsuarioLogado usuarioLogado,
                                  EspelhoRepository espelhoRepository,
                                  EspelhoItemRepository espelhoItemRepository,
                                  FaturaEventoRepository eventoRepository,
                                  FaturaTransicaoService transicaoService,
                                  FaturaConsultaService consultaService,
                                  AndamentoGlosaService andamentoGlosaService) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.eventoRepository = eventoRepository;
        this.transicaoService = transicaoService;
        this.consultaService = consultaService;
        this.andamentoGlosaService = andamentoGlosaService;
    }

    /** T26-BE1: faturas EM_AUDITORIA, da mais antiga para a mais recente pelo envio a auditoria. */
    @Transactional(readOnly = true)
    public List<AuditoriaResumoResponse> listar() {
        usuarioLogado.exigirPerfil(PerfilUsuario.AUDITORIA);
        List<Espelho> espelhos = espelhoRepository
                .findBySituacaoFaturaInOrderByDataEnvioAscIdEspelhoAsc(EnumSet.of(SituacaoFatura.EM_AUDITORIA));
        if (espelhos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = espelhos.stream().map(Espelho::getIdEspelho).toList();
        Map<Long, List<EspelhoItem>> itens = itensPorEspelho(ids);
        Map<Long, List<FaturaEvento>> eventos = eventosPorEspelho(ids);

        return espelhos.stream()
                .map(e -> AuditoriaResumoResponse.fromEntity(e,
                        itens.getOrDefault(e.getIdEspelho(), List.of()),
                        eventos.getOrDefault(e.getIdEspelho(), List.of())))
                .sorted(Comparator.comparing(AuditoriaResumoResponse::dataRecebimentoAuditoria,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditoriaResumoResponse::id))
                .toList();
    }

    /** T26-BE1: detalhe de fatura a partir de EM_AUDITORIA (inclusive AUDITADA, para o historico). */
    @Transactional(readOnly = true)
    public AuditoriaDetalheResponse detalhar(Long id) {
        usuarioLogado.exigirPerfil(PerfilUsuario.AUDITORIA);
        espelhoRepository.findById(id)
                .filter(e -> e.getSituacaoFatura() != null && VISIVEIS_NO_DETALHE.contains(e.getSituacaoFatura()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        return new AuditoriaDetalheResponse(consultaService.montarDetalhe(id), andamentoGlosaService.daFatura(id));
    }

    /** T26-BE2: marca a fatura como visualizada (EM_AUDITORIA -> AUDITADA). Responsavel obrigatorio. */
    @Transactional
    public FaturaDetalheResponse visualizar(Long id, EnviarFaturaRequest request) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.AUDITORIA);
        Espelho espelho = espelhoRepository.buscarComLockPorId(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (espelho.getSituacaoFatura() != SituacaoFatura.EM_AUDITORIA) {
            throw new ConflitoException("Esta acao nao e permitida na situacao atual da fatura");
        }
        transicaoService.registrar(id, TipoFaturaEvento.VISUALIZADA, usuario,
                request == null ? null : request.responsavel());
        return consultaService.montarDetalhe(id);
    }

    private Map<Long, List<EspelhoItem>> itensPorEspelho(List<Long> ids) {
        return espelhoItemRepository.buscarComDadosPorEspelhos(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getEspelho().getIdEspelho()));
    }

    private Map<Long, List<FaturaEvento>> eventosPorEspelho(List<Long> ids) {
        return eventoRepository.findByEspelhoIdEspelhoInOrderByDataEventoAscIdFaturaEventoAsc(ids).stream()
                .collect(Collectors.groupingBy(evento -> evento.getEspelho().getIdEspelho()));
    }
}
