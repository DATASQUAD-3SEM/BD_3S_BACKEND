package fatec.fusex.nexus.glosaOcs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcsRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.glosaOcs.dto.GlosaDetalheResponse;
import fatec.fusex.nexus.glosaOcs.dto.GlosaGuiaResponse;
import fatec.fusex.nexus.glosaOcs.dto.GlosaResumoResponse;
import fatec.fusex.nexus.ocs.ContaOcsService;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaConsultaService;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;
import fatec.fusex.nexus.usuario.PerfilUsuario;

/** Glosas recebidas pela OCS (T27-BE1): somente leitura. A decisao e a T27-BE2. */
@Service
public class GlosaOcsService {

    private static final Set<SituacaoFatura> VISIVEIS_NO_DETALHE =
            EnumSet.of(SituacaoFatura.AGUARDANDO_OCS, SituacaoFatura.EM_AUDITORIA, SituacaoFatura.AUDITADA);

    private final UsuarioLogado usuarioLogado;
    private final ContaOcsService contaOcsService;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final EspelhoItemRodadaOcsRepository rodadaRepository;
    private final FaturaEventoRepository eventoRepository;
    private final FaturaConsultaService consultaService;

    public GlosaOcsService(UsuarioLogado usuarioLogado,
                           ContaOcsService contaOcsService,
                           EspelhoRepository espelhoRepository,
                           EspelhoItemRepository espelhoItemRepository,
                           EspelhoItemRodadaOcsRepository rodadaRepository,
                           FaturaEventoRepository eventoRepository,
                           FaturaConsultaService consultaService) {
        this.usuarioLogado = usuarioLogado;
        this.contaOcsService = contaOcsService;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.rodadaRepository = rodadaRepository;
        this.eventoRepository = eventoRepository;
        this.consultaService = consultaService;
    }

    /** Faturas AGUARDANDO_OCS desta OCS sem decisao na rodada atual, do envio mais antigo ao mais recente. */
    @Transactional(readOnly = true)
    public List<GlosaResumoResponse> listar() {
        usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();
        LocalDateTime agora = LocalDateTime.now(Beneficiario.FUSO);

        List<Espelho> espelhos = espelhoRepository
                .findBySituacaoFaturaInOrderByDataEnvioAscIdEspelhoAsc(EnumSet.of(SituacaoFatura.AGUARDANDO_OCS))
                .stream()
                .filter(e -> e.getOcs().getIdOcs().equals(idOcs))
                .toList();
        if (espelhos.isEmpty()) {
            return List.of();
        }

        List<Long> ids = espelhos.stream().map(Espelho::getIdEspelho).toList();
        Map<Long, List<EspelhoItem>> itens = itensPorEspelho(ids);
        Map<Long, List<FaturaEvento>> eventos = eventosPorEspelho(ids);

        return espelhos.stream()
                .map(e -> {
                    List<EspelhoItem> itensDaFatura = itens.getOrDefault(e.getIdEspelho(), List.of());
                    List<FaturaEvento> evs = eventos.getOrDefault(e.getIdEspelho(), List.of());
                    FaturaEvento envio = ultimoEnvio(evs);
                    if (envio == null || rodadaDecidida(evs, envio)) {
                        return null;
                    }
                    List<EspelhoItem> daRodada = linhasDaRodada(itensDaFatura, envio);
                    if (daRodada.isEmpty()) {
                        return null;
                    }
                    LocalDateTime prazo = prazoDoEnvio(envio);
                    return GlosaResumoResponse.fromEntity(e, daRodada,
                            envio.getDataEvento(), prazo, agora.isAfter(prazo));
                })
                .filter(r -> r != null)
                .sorted(Comparator.comparing(GlosaResumoResponse::dataEnvioOcs))
                .toList();
    }

    /** Detalhe de fatura desta OCS que teve glosa e chegou a AGUARDANDO_OCS. */
    @Transactional(readOnly = true)
    public GlosaDetalheResponse detalhar(Long id) {
        usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();
        LocalDateTime agora = LocalDateTime.now(Beneficiario.FUSO);

        Espelho espelho = espelhoRepository.findById(id)
                .filter(e -> e.getSituacaoFatura() != null && VISIVEIS_NO_DETALHE.contains(e.getSituacaoFatura()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (!espelho.getOcs().getIdOcs().equals(idOcs)) {
            throw new AcessoNegadoException("Esta fatura pertence a outra OCS");
        }

        List<EspelhoItem> itens = espelhoItemRepository.buscarComDadosPorEspelho(id);
        List<FaturaEvento> eventos = eventoRepository.findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(id);
        FaturaEvento envio = ultimoEnvio(eventos);
        if (envio == null) {
            throw new RecursoNaoEncontradoException("Fatura sem glosa para esta OCS");
        }
        List<EspelhoItem> comGlosa = linhasDaRodada(itens, envio);
        if (comGlosa.isEmpty()) {
            throw new RecursoNaoEncontradoException("Fatura sem glosa para esta OCS");
        }

        LocalDateTime prazo = prazoDoEnvio(envio);
        FaturaEvento ciencia = eventoApos(eventos, envio, TipoFaturaEvento.CIENTE_OCS);
        BigDecimal informado = ControleFluxoFatura.valorInformado(comGlosa).setScale(2);
        BigDecimal valorFinal = ControleFluxoFatura.valorFinal(comGlosa);
        BigDecimal glosa = valorFinal == null ? null : informado.subtract(valorFinal).setScale(2);

        return new GlosaDetalheResponse(
                espelho.getIdEspelho(),
                espelho.getNumeroFatura(),
                envio.getDataEvento(),
                prazo,
                ciencia == null && agora.isAfter(prazo),
                ciencia == null ? null : ciencia.getDataEvento(),
                ciencia == null ? null : ciencia.getResponsavel(),
                informado,
                glosa,
                valorFinal,
                comGlosa.stream().map(GlosaGuiaResponse::fromEntity).toList(),
                consultaService.montarDetalhe(id).controleFluxo());
    }

    static FaturaEvento ultimoEnvio(List<FaturaEvento> eventos) {
        FaturaEvento envio = null;
        for (FaturaEvento evento : eventos) {
            if (evento.getTipo() == TipoFaturaEvento.ENVIADA_OCS) {
                envio = evento;
            }
        }
        return envio;
    }

    /** Rodada decidida: CIENTE_OCS ou CONTESTADA_OCS depois do envio mais recente. */
    static boolean rodadaDecidida(List<FaturaEvento> eventos, FaturaEvento envio) {
        return eventoApos(eventos, envio, TipoFaturaEvento.CIENTE_OCS) != null
                || eventoApos(eventos, envio, TipoFaturaEvento.CONTESTADA_OCS) != null;
    }

    /**
     * Linhas glosadas da rodada atual (as gravadas em EspelhoItemRodadaOcs no envio mais recente). Sem rodada gravada
     * (dados anteriores as rodadas), as linhas com glosa pendente: glosa diferente de zero e sem "Ok" da OCS.
     */
    private List<EspelhoItem> linhasDaRodada(List<EspelhoItem> itens, FaturaEvento envio) {
        Set<Long> daRodada = rodadaRepository.findByEnvioEventoIdFaturaEvento(envio.getIdFaturaEvento()).stream()
                .map(rodada -> rodada.getEspelhoItem().getIdEspelhoItem())
                .collect(Collectors.toSet());
        if (daRodada.isEmpty()) {
            return itens.stream()
                    .filter(item -> ControleFluxoFatura.glosaDaLinha(item).signum() != 0
                            && !"OK".equals(item.getDecisaoOcs()))
                    .toList();
        }
        return itens.stream().filter(item -> daRodada.contains(item.getIdEspelhoItem())).toList();
    }

    private static LocalDateTime prazoDoEnvio(FaturaEvento envio) {
        return FaturaTransicaoService.somarDiasUteis(envio.getDataEvento(),
                FaturaTransicaoService.PRAZO_OCS_DIAS_UTEIS);
    }

    private static FaturaEvento eventoApos(List<FaturaEvento> eventos, FaturaEvento envio, TipoFaturaEvento tipo) {
        FaturaEvento achado = null;
        for (FaturaEvento e : eventos.subList(eventos.indexOf(envio) + 1, eventos.size())) {
            if (e.getTipo() == tipo) {
                achado = e;
            }
        }
        return achado;
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
