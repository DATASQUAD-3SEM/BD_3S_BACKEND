package fatec.fusex.nexus.glosaOcs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcs;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcsRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.glosaOcs.dto.DecisaoGlosaRequest;
import fatec.fusex.nexus.glosaOcs.dto.DecisaoGlosaRequest.DecisaoGuiaRequest;
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
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/** Glosas recebidas pela OCS: T27-BE1 (leitura) e T27-BE2 (decisao por Guia). */
@Service
public class GlosaOcsService {

    private static final Set<SituacaoFatura> VISIVEIS_NO_DETALHE =
            EnumSet.of(SituacaoFatura.AGUARDANDO_OCS, SituacaoFatura.EM_AUDITORIA, SituacaoFatura.AUDITADA);
    private static final String OK = "OK";
    private static final String CONTESTADA = "CONTESTADA";
    private static final int MOTIVO_MINIMO = 10;
    private static final int MOTIVO_MAXIMO = 500;
    private static final int RESPONSAVEL_MINIMO = 3;
    private static final int RESPONSAVEL_MAXIMO = 100;

    private final UsuarioLogado usuarioLogado;
    private final ContaOcsService contaOcsService;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final EspelhoItemRodadaOcsRepository rodadaRepository;
    private final FaturaEventoRepository eventoRepository;
    private final FaturaConsultaService consultaService;
    private final FaturaTransicaoService transicaoService;

    public GlosaOcsService(UsuarioLogado usuarioLogado,
                           ContaOcsService contaOcsService,
                           EspelhoRepository espelhoRepository,
                           EspelhoItemRepository espelhoItemRepository,
                           EspelhoItemRodadaOcsRepository rodadaRepository,
                           FaturaEventoRepository eventoRepository,
                           FaturaConsultaService consultaService,
                           FaturaTransicaoService transicaoService) {
        this.usuarioLogado = usuarioLogado;
        this.contaOcsService = contaOcsService;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.rodadaRepository = rodadaRepository;
        this.eventoRepository = eventoRepository;
        this.consultaService = consultaService;
        this.transicaoService = transicaoService;
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

    /**
     * T27-BE2: registra Ok ou Contestar por Guia. Sem contestacao, grava CIENTE_OCS (fatura continua
     * AGUARDANDO_OCS). Com contestacao, grava CONTESTADA_OCS (fatura volta para EM_LISURA) e apaga o
     * contrato das linhas contestadas. Tudo numa transacao.
     */
    @Transactional
    public FaturaDetalheResponse decidir(Long id, DecisaoGlosaRequest request) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();
        LocalDateTime agora = LocalDateTime.now(Beneficiario.FUSO);

        Espelho espelho = espelhoRepository.buscarComLockPorId(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (!espelho.getOcs().getIdOcs().equals(idOcs)) {
            throw new AcessoNegadoException("Esta fatura pertence a outra OCS");
        }
        if (espelho.getSituacaoFatura() != SituacaoFatura.AGUARDANDO_OCS) {
            throw new RecursoNaoEncontradoException("Fatura nao esta aguardando a decisao da OCS");
        }

        List<FaturaEvento> eventos = eventoRepository.findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(id);
        FaturaEvento envio = ultimoEnvio(eventos);
        if (envio == null) {
            throw new RecursoNaoEncontradoException("Fatura sem envio a OCS");
        }
        if (rodadaDecidida(eventos, envio)) {
            throw new ConflitoException("A OCS já decidiu as glosas desta fatura.");
        }
        LocalDateTime prazo = prazoDoEnvio(envio);
        if (agora.isAfter(prazo)) {
            throw new ConflitoException("O prazo de 3 dias úteis já venceu; a fatura já pode seguir para a auditoria.");
        }

        String responsavel = validarResponsavel(request == null ? null : request.responsavel());
        List<EspelhoItem> itens = espelhoItemRepository.buscarComDadosPorEspelho(id);
        List<EspelhoItem> pendentes = itens.stream()
                .filter(item -> ControleFluxoFatura.glosaDaLinha(item).signum() != 0 && !OK.equals(item.getDecisaoOcs()))
                .toList();
        if (pendentes.isEmpty()) {
            throw new ConflitoException("Não há glosa pendente nesta fatura.");
        }

        Map<Long, DecisaoGuiaRequest> decisoes = validarDecisoes(request.decisoes(), pendentes);
        boolean houveContestacao = decisoes.values().stream().anyMatch(d -> CONTESTADA.equals(d.decisao()));
        TipoFaturaEvento tipo = houveContestacao ? TipoFaturaEvento.CONTESTADA_OCS : TipoFaturaEvento.CIENTE_OCS;
        FaturaEvento evento = transicaoService.registrar(id, tipo, usuario, responsavel);

        Map<Long, EspelhoItemRodadaOcs> rodadas = rodadaRepository
                .findByEnvioEventoIdFaturaEvento(envio.getIdFaturaEvento()).stream()
                .collect(Collectors.toMap(r -> r.getEspelhoItem().getIdEspelhoItem(), r -> r));
        for (EspelhoItem item : pendentes) {
            DecisaoGuiaRequest decisao = decisoes.get(item.getGuiaProcedimento().getGuia().getIdGuia());
            boolean contestada = CONTESTADA.equals(decisao.decisao());
            String motivo = contestada ? decisao.motivo().trim() : null;
            item.setDecisaoOcs(decisao.decisao());
            item.setMotivoContestacaoOcs(motivo);
            if (contestada) {
                item.setValorContratoSessao(null);
                item.setOrigemContrato(null);
            }
            EspelhoItemRodadaOcs rodada = rodadas.get(item.getIdEspelhoItem());
            if (rodada != null) {
                rodada.setDecisaoOcs(decisao.decisao());
                rodada.setMotivoContestacaoOcs(motivo);
                rodada.setDecisaoEvento(evento);
            }
        }
        return consultaService.montarDetalhe(id);
    }

    /** Cobertura exata: uma decisao por Guia com glosa pendente, sem repeticao e sem Guia a mais. */
    private Map<Long, DecisaoGuiaRequest> validarDecisoes(List<DecisaoGuiaRequest> lista, List<EspelhoItem> pendentes) {
        if (lista == null || lista.isEmpty()) {
            throw new DadosInvalidosException("decisoes", "É preciso decidir todas as guias glosadas.");
        }
        Set<Long> guiasPendentes = pendentes.stream()
                .map(item -> item.getGuiaProcedimento().getGuia().getIdGuia())
                .collect(Collectors.toSet());
        Set<Long> vistas = new HashSet<>();
        for (DecisaoGuiaRequest d : lista) {
            if (d == null || d.guiaId() == null || !vistas.add(d.guiaId())) {
                throw new DadosInvalidosException("decisoes", "É preciso decidir todas as guias glosadas.");
            }
            if (!OK.equals(d.decisao()) && !CONTESTADA.equals(d.decisao())) {
                throw new RegraDeNegocioException("A decisao deve ser OK ou CONTESTADA.");
            }
            if (CONTESTADA.equals(d.decisao())) {
                String motivo = d.motivo() == null ? "" : d.motivo().trim();
                if (motivo.length() < MOTIVO_MINIMO || motivo.length() > MOTIVO_MAXIMO) {
                    throw new RegraDeNegocioException("O motivo da contestação deve ter de 10 a 500 caracteres.");
                }
            }
        }
        if (!vistas.equals(guiasPendentes)) {
            throw new DadosInvalidosException("decisoes", "É preciso decidir todas as guias glosadas.");
        }
        return lista.stream().collect(Collectors.toMap(DecisaoGuiaRequest::guiaId, d -> d));
    }

    private String validarResponsavel(String responsavel) {
        String nome = responsavel == null ? "" : responsavel.trim();
        if (nome.length() < RESPONSAVEL_MINIMO || nome.length() > RESPONSAVEL_MAXIMO) {
            throw new DadosInvalidosException("responsavel", "Informe o nome do responsável (mínimo de 3 caracteres).");
        }
        return nome;
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
