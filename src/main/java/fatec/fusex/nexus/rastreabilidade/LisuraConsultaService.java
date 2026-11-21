package fatec.fusex.nexus.rastreabilidade;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.rastreabilidade.dto.FaturaLisuraResumoResponse;
import fatec.fusex.nexus.setorFatura.FaturaConsultaService;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@Service
public class LisuraConsultaService {

    private static final Set<SituacaoFatura> NA_LISURA =
            EnumSet.of(SituacaoFatura.EM_LISURA, SituacaoFatura.AGUARDANDO_OCS);
    private static final Set<SituacaoFatura> A_PARTIR_DA_LISURA =
            EnumSet.of(SituacaoFatura.EM_LISURA, SituacaoFatura.AGUARDANDO_OCS,
                    SituacaoFatura.EM_AUDITORIA, SituacaoFatura.AUDITADA);

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final FaturaEventoRepository eventoRepository;
    private final FaturaConsultaService consultaService;

    public LisuraConsultaService(UsuarioLogado usuarioLogado,
                                 EspelhoRepository espelhoRepository,
                                 EspelhoItemRepository espelhoItemRepository,
                                 FaturaEventoRepository eventoRepository,
                                 FaturaConsultaService consultaService) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.eventoRepository = eventoRepository;
        this.consultaService = consultaService;
    }

    @Transactional(readOnly = true)
    public List<FaturaLisuraResumoResponse> listar() {
        usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);

        List<Espelho> espelhos = espelhoRepository.findBySituacaoFaturaInOrderByDataEnvioAscIdEspelhoAsc(NA_LISURA);
        if (espelhos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = espelhos.stream().map(Espelho::getIdEspelho).toList();
        Map<Long, List<EspelhoItem>> itensPorEspelho = espelhoItemRepository.buscarComDadosPorEspelhos(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getEspelho().getIdEspelho()));
        Map<Long, List<FaturaEvento>> eventosPorEspelho = eventoRepository
                .findByEspelhoIdEspelhoInOrderByDataEventoAscIdFaturaEventoAsc(ids).stream()
                .collect(Collectors.groupingBy(evento -> evento.getEspelho().getIdEspelho()));

        LocalDateTime agora = LocalDateTime.now(Beneficiario.FUSO);
        return espelhos.stream()
                .map(espelho -> FaturaLisuraResumoResponse.fromEntity(
                        espelho,
                        itensPorEspelho.getOrDefault(espelho.getIdEspelho(), List.of()),
                        eventosPorEspelho.getOrDefault(espelho.getIdEspelho(), List.of()),
                        agora))
                .toList();
    }

    @Transactional(readOnly = true)
    public FaturaDetalheResponse detalhar(Long id) {
        usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        espelhoRepository.findById(id)
                .filter(e -> A_PARTIR_DA_LISURA.contains(e.getSituacaoFatura()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        return consultaService.montarDetalhe(id);
    }
}