package fatec.fusex.nexus.andamentoGlosa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse;
import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse.ItemAndamento;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcs;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;

/**
 * T26-BE4 (regra 13.2): andamento da glosa por Guia, montado das rodadas da OCS (EspelhoItemRodadaOcs) e dos
 * eventos da fatura. Usado pelo detalhe da auditoria (T26-BE1) e pelo verso da Guia (T32-BE1). So leitura.
 */
@Service
public class AndamentoGlosaService {

    public static final String ACEITA_PELA_OCS = "ACEITA_PELA_OCS";
    public static final String PRAZO_VENCIDO = "PRAZO_VENCIDO";
    public static final String CONTESTACAO_ACATADA = "CONTESTACAO_ACATADA";
    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";

    private static final Map<String, String> SELOS = Map.of(
            ACEITA_PELA_OCS, "Aceita pela OCS",
            PRAZO_VENCIDO, "Prazo vencido",
            CONTESTACAO_ACATADA, "Contestação acatada",
            EM_ANDAMENTO, "Em andamento");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final AndamentoGlosaRepository andamentoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final FaturaEventoRepository eventoRepository;

    public AndamentoGlosaService(AndamentoGlosaRepository andamentoRepository,
                                 EspelhoItemRepository espelhoItemRepository,
                                 FaturaEventoRepository eventoRepository) {
        this.andamentoRepository = andamentoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.eventoRepository = eventoRepository;
    }

    /** Andamento de cada Guia da fatura que foi a OCS em alguma rodada, na ordem do numero da Guia. */
    @Transactional(readOnly = true)
    public List<AndamentoGlosaResponse> daFatura(Long faturaId) {
        return daFatura(faturaId, LocalDateTime.now(Beneficiario.FUSO));
    }

    /** Andamento de uma Guia da fatura; vazio (sem itens) quando a Guia nunca foi a OCS. */
    @Transactional(readOnly = true)
    public AndamentoGlosaResponse daGuia(Long faturaId, Long guiaId) {
        return daFatura(faturaId).stream()
                .filter(andamento -> andamento.guiaId().equals(guiaId))
                .findFirst()
                .orElse(new AndamentoGlosaResponse(guiaId, null, null, null, List.of()));
    }

    List<AndamentoGlosaResponse> daFatura(Long faturaId, LocalDateTime agora) {
        List<EspelhoItemRodadaOcs> rodadas = andamentoRepository.rodadasDaFatura(faturaId);
        if (rodadas.isEmpty()) {
            return List.of();
        }
        List<FaturaEvento> eventos = eventoRepository.findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(faturaId);
        Map<Long, EspelhoItem> itensAtuais = espelhoItemRepository.buscarComDadosPorEspelho(faturaId).stream()
                .collect(Collectors.toMap(EspelhoItem::getIdEspelhoItem, Function.identity()));

        Map<Guia, List<EspelhoItemRodadaOcs>> porGuia = rodadas.stream()
                .collect(Collectors.groupingBy(r -> r.getEspelhoItem().getGuiaProcedimento().getGuia(),
                        LinkedHashMap::new, Collectors.toList()));

        return porGuia.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Guia::getNumero)))
                .map(e -> daGuia(e.getKey(), e.getValue(), eventos, itensAtuais, agora))
                .toList();
    }

    private AndamentoGlosaResponse daGuia(Guia guia, List<EspelhoItemRodadaOcs> linhas, List<FaturaEvento> eventos,
                                          Map<Long, EspelhoItem> itensAtuais, LocalDateTime agora) {
        // Uma rodada por envio a OCS (as linhas ja vem em ordem de envio).
        Map<FaturaEvento, List<EspelhoItemRodadaOcs>> porEnvio = linhas.stream()
                .collect(Collectors.groupingBy(EspelhoItemRodadaOcs::getEnvioEvento, LinkedHashMap::new,
                        Collectors.toList()));
        List<FaturaEvento> envios = new ArrayList<>(porEnvio.keySet());
        List<ItemAndamento> itens = new ArrayList<>();

        for (int i = 0; i < envios.size(); i++) {
            FaturaEvento envio = envios.get(i);
            List<EspelhoItemRodadaOcs> rodada = porEnvio.get(envio);
            LocalDateTime prazo = prazo(envio);

            if (i == 0) {
                itens.add(new ItemAndamento(envio.getDataEvento(),
                        "Lisura concluída e glosa enviada à OCS pela rastreabilidade — responsável: "
                                + envio.getResponsavel() + ". Prazo de 3 dias úteis, até "
                                + prazo.format(DATA) + "."));
            } else {
                List<EspelhoItemRodadaOcs> anterior = porEnvio.get(envios.get(i - 1));
                itens.addAll(retiradas(anterior, rodada, itensAtuais, envio));
                itens.add(new ItemAndamento(envio.getDataEvento(),
                        "Revista pela rastreabilidade e reenviada à OCS — " + reenvio(anterior, rodada)
                                + "; responsável: " + envio.getResponsavel() + ". Prazo até "
                                + prazo.format(DATA) + "."));
            }

            EspelhoItemRodadaOcs decidida = rodada.stream()
                    .filter(r -> r.getDecisaoOcs() != null).findFirst().orElse(null);
            if (decidida != null) {
                itens.add(decisao(decidida));
            } else if (i == envios.size() - 1 && agora.isAfter(prazo)) {
                itens.add(new ItemAndamento(prazo,
                        "Prazo vencido sem resposta da OCS. A glosa segue como lisurada pela rastreabilidade."));
            }
        }

        FaturaEvento ultimoEnvio = envios.get(envios.size() - 1);
        List<EspelhoItemRodadaOcs> ultima = porEnvio.get(ultimoEnvio);
        String situacao = situacaoFinal(ultima, ultimoEnvio, itensAtuais, agora);
        if (CONTESTACAO_ACATADA.equals(situacao)) {
            // Revisao sem reenvio: registrada quando a rastreabilidade envia a fatura a auditoria.
            LocalDateTime contestacao = ultima.get(0).getDecisaoEvento().getDataEvento();
            eventos.stream()
                    .filter(e -> e.getTipo() == TipoFaturaEvento.ENVIADA_AUDITORIA
                            && e.getDataEvento().isAfter(contestacao))
                    .findFirst()
                    .ifPresent(auditoria -> itens.addAll(retiradas(ultima, List.of(), itensAtuais, auditoria)));
        }

        return new AndamentoGlosaResponse(guia.getIdGuia(), guia.getNumero(), situacao, SELOS.get(situacao), itens);
    }

    private String situacaoFinal(List<EspelhoItemRodadaOcs> ultima, FaturaEvento envio,
                                 Map<Long, EspelhoItem> itensAtuais, LocalDateTime agora) {
        String decisao = ultima.stream().map(EspelhoItemRodadaOcs::getDecisaoOcs)
                .filter(Objects::nonNull).findFirst().orElse(null);
        if ("OK".equals(decisao)) {
            return ACEITA_PELA_OCS;
        }
        if (decisao == null) {
            return agora.isAfter(prazo(envio)) ? PRAZO_VENCIDO : EM_ANDAMENTO;
        }
        boolean glosaRetirada = ultima.stream()
                .map(r -> itensAtuais.get(r.getEspelhoItem().getIdEspelhoItem()))
                .allMatch(item -> item != null && semGlosa(item));
        return glosaRetirada ? CONTESTACAO_ACATADA : EM_ANDAMENTO;
    }

    private ItemAndamento decisao(EspelhoItemRodadaOcs decidida) {
        FaturaEvento evento = decidida.getDecisaoEvento();
        String texto = "OK".equals(decidida.getDecisaoOcs())
                ? "OCS de acordo (Ok) — responsável: " + evento.getResponsavel() + "."
                : "Contestada pela OCS — responsável: " + evento.getResponsavel() + ". Motivo: \""
                        + decidida.getMotivoContestacaoOcs() + "\"";
        return new ItemAndamento(evento.getDataEvento(), texto);
    }

    /** Linhas da rodada anterior que nao voltaram a OCS porque a revisao retirou a glosa. */
    private List<ItemAndamento> retiradas(List<EspelhoItemRodadaOcs> anterior, List<EspelhoItemRodadaOcs> proxima,
                                          Map<Long, EspelhoItem> itensAtuais, FaturaEvento quando) {
        Set<Long> reenviados = proxima.stream().map(r -> r.getEspelhoItem().getIdEspelhoItem())
                .collect(Collectors.toSet());
        return anterior.stream()
                .filter(r -> !reenviados.contains(r.getEspelhoItem().getIdEspelhoItem()))
                .map(r -> itensAtuais.get(r.getEspelhoItem().getIdEspelhoItem()))
                .filter(item -> item != null && semGlosa(item))
                .map(item -> new ItemAndamento(quando.getDataEvento(),
                        "Revista pela rastreabilidade — " + procedimento(item) + ": glosa retirada (contrato igual"
                                + " ao informado, " + reais(item.getValorContratoSessao()) + "). Responsável: "
                                + quando.getResponsavel() + "."))
                .toList();
    }

    private String reenvio(List<EspelhoItemRodadaOcs> anterior, List<EspelhoItemRodadaOcs> rodada) {
        Map<Long, BigDecimal> valorAnterior = new LinkedHashMap<>();
        anterior.forEach(r -> valorAnterior.put(r.getEspelhoItem().getIdEspelhoItem(), r.getValorContratoSessao()));
        return rodada.stream().map(r -> {
            BigDecimal antes = valorAnterior.get(r.getEspelhoItem().getIdEspelhoItem());
            BigDecimal agora = r.getValorContratoSessao();
            String nome = procedimento(r.getEspelhoItem());
            if (antes == null) {
                return nome + ": contrato de " + reais(agora);
            }
            return antes.compareTo(agora) == 0
                    ? nome + ": contrato mantido em " + reais(agora)
                    : nome + ": contrato alterado de " + reais(antes) + " para " + reais(agora);
        }).collect(Collectors.joining("; "));
    }

    private static boolean semGlosa(EspelhoItem item) {
        BigDecimal glosa = ControleFluxoFatura.glosaDaLinha(item);
        return glosa != null && glosa.signum() == 0;
    }

    private static LocalDateTime prazo(FaturaEvento envio) {
        return FaturaTransicaoService.somarDiasUteis(envio.getDataEvento(), FaturaTransicaoService.PRAZO_OCS_DIAS_UTEIS);
    }

    private static String procedimento(EspelhoItem item) {
        return item.getGuiaProcedimento().getDescricaoProcedimento();
    }

    private static String reais(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }
}
