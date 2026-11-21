package fatec.fusex.nexus.setorFatura;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.setorFatura.dto.FaturaAuditadaResponse;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.setorFatura.dto.FaturaResumoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@Service
public class FaturaConsultaService {

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final FaturaEventoRepository eventoRepository;

    public FaturaConsultaService(UsuarioLogado usuarioLogado,
                                 EspelhoRepository espelhoRepository,
                                 EspelhoItemRepository espelhoItemRepository,
                                 FaturaEventoRepository eventoRepository) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<FaturaResumoResponse> listar() {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_FATURA);

        List<Espelho> espelhos = espelhoRepository.buscarComSituacaoFaturaExceto(SituacaoFatura.AUDITADA);
        if (espelhos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = espelhos.stream().map(Espelho::getIdEspelho).toList();
        Map<Long, List<EspelhoItem>> itensPorEspelho = espelhoItemRepository.buscarComDadosPorEspelhos(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getEspelho().getIdEspelho()));
        Map<Long, List<FaturaEvento>> eventosPorEspelho = eventoRepository
                .findByEspelhoIdEspelhoInOrderByDataEventoAscIdFaturaEventoAsc(ids).stream()
                .collect(Collectors.groupingBy(evento -> evento.getEspelho().getIdEspelho()));

        return espelhos.stream()
                .map(espelho -> FaturaResumoResponse.fromEntity(
                        espelho, itensPorEspelho.getOrDefault(espelho.getIdEspelho(), List.of()),
                        eventosPorEspelho.getOrDefault(espelho.getIdEspelho(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public FaturaDetalheResponse detalhar(Long id) {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_FATURA);
        return montarDetalhe(id);
    }

    @Transactional(readOnly = true)
    public List<FaturaAuditadaResponse> listarAuditadas() {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_FATURA);

        List<Espelho> espelhos = espelhoRepository
                .findBySituacaoFaturaInOrderByDataEnvioAscIdEspelhoAsc(List.of(SituacaoFatura.AUDITADA));
        if (espelhos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = espelhos.stream().map(Espelho::getIdEspelho).toList();
        Map<Long, List<EspelhoItem>> itensPorEspelho = espelhoItemRepository.buscarComDadosPorEspelhos(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getEspelho().getIdEspelho()));
        Map<Long, LocalDateTime> visualizacaoPorEspelho = dataVisualizacaoPorEspelho(ids);

        return espelhos.stream()
                .map(espelho -> FaturaAuditadaResponse.fromEntity(
                        espelho,
                        itensPorEspelho.getOrDefault(espelho.getIdEspelho(), List.of()),
                        visualizacaoPorEspelho.get(espelho.getIdEspelho())))
                .sorted(Comparator.comparing(FaturaAuditadaResponse::dataVisualizacao,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FaturaAuditadaResponse::id, Comparator.reverseOrder()))
                .toList();
    }

    private Map<Long, LocalDateTime> dataVisualizacaoPorEspelho(List<Long> ids) {
        Map<Long, LocalDateTime> porEspelho = new HashMap<>();
        for (FaturaEvento evento : eventoRepository
                .findByEspelhoIdEspelhoInOrderByDataEventoAscIdFaturaEventoAsc(ids)) {
            if (evento.getTipo() == TipoFaturaEvento.VISUALIZADA) {
                porEspelho.put(evento.getEspelho().getIdEspelho(), evento.getDataEvento());
            }
        }
        return porEspelho;
    }

    public FaturaDetalheResponse montarDetalhe(Long id) {
        Espelho espelho = espelhoRepository.findById(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));

        List<EspelhoItem> itens = espelhoItemRepository.buscarComDadosPorEspelho(id);
        List<FaturaEvento> eventos = eventoRepository.findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(id);
        List<EtapaFluxo> etapas = ControleFluxoFatura.calcular(
                espelho.getDataEnvio(), eventos, ControleFluxoFatura.valorInformado(itens),
                ControleFluxoFatura.valorFinal(itens));

        return FaturaDetalheResponse.fromEntity(espelho, itens, eventos, etapas,
                LocalDateTime.now(fatec.fusex.nexus.beneficiario.Beneficiario.FUSO));
    }
}