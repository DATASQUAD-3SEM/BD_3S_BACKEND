package fatec.fusex.nexus.versoGuia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.andamentoGlosa.AndamentoGlosaRepository;
import fatec.fusex.nexus.andamentoGlosa.AndamentoGlosaService;
import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.versoGuia.dto.VersoFaturaDetalheResponse;
import fatec.fusex.nexus.versoGuia.dto.VersoFaturaDetalheResponse.VersoGuiaResponse;
import fatec.fusex.nexus.versoGuia.dto.VersoFaturaResponse;

/**
 * T32-BE1 (regra 15): versos das Guias glosadas. Uma Guia tem verso quando ao menos uma linha dela foi a OCS em
 * alguma rodada, e a fatura ja foi liberada (Aguardando a OCS com ciencia ou prazo vencido, Em auditoria ou
 * Auditada). So leitura: imprimir nao grava nada.
 */
@Service
public class VersoGuiaService {

    private static final Set<SituacaoFatura> COM_VERSO_POSSIVEL =
            EnumSet.of(SituacaoFatura.AGUARDANDO_OCS, SituacaoFatura.EM_AUDITORIA, SituacaoFatura.AUDITADA);

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final FaturaEventoRepository eventoRepository;
    private final AndamentoGlosaRepository andamentoRepository;
    private final AndamentoGlosaService andamentoGlosaService;
    private final VersoPdf versoPdf;

    public VersoGuiaService(UsuarioLogado usuarioLogado,
                            EspelhoRepository espelhoRepository,
                            EspelhoItemRepository espelhoItemRepository,
                            FaturaEventoRepository eventoRepository,
                            AndamentoGlosaRepository andamentoRepository,
                            AndamentoGlosaService andamentoGlosaService,
                            VersoPdf versoPdf) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.eventoRepository = eventoRepository;
        this.andamentoRepository = andamentoRepository;
        this.andamentoGlosaService = andamentoGlosaService;
        this.versoPdf = versoPdf;
    }

    /** Faturas liberadas com ao menos uma Guia glosada (que foi a OCS em alguma rodada). */
    @Transactional(readOnly = true)
    public List<VersoFaturaResponse> listar() {
        usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        LocalDateTime agora = agora();
        List<VersoFaturaResponse> faturas = new ArrayList<>();
        for (Espelho espelho : espelhoRepository.findBySituacaoFaturaInOrderByDataEnvioAscIdEspelhoAsc(COM_VERSO_POSSIVEL)) {
            if (!liberada(espelho, agora)) {
                continue;
            }
            List<VersoGuia> versos = versos(espelho);
            if (!versos.isEmpty()) {
                BigDecimal glosa = versos.stream().map(VersoGuia::totalGlosa).reduce(BigDecimal.ZERO, BigDecimal::add);
                faturas.add(new VersoFaturaResponse(espelho.getIdEspelho(), espelho.getNumeroFatura(),
                        espelho.getOcs().getNome(), espelho.getSituacaoFatura().name(), versos.size(), glosa));
            }
        }
        return faturas;
    }

    @Transactional(readOnly = true)
    public VersoFaturaDetalheResponse detalhar(Long faturaId) {
        usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        Espelho espelho = faturaLiberada(faturaId);
        List<VersoGuiaResponse> guias = versos(espelho).stream()
                .map(v -> new VersoGuiaResponse(v.guiaId(), v.guiaNumero(), v.paciente(),
                        v.andamento().situacaoFinal(), v.andamento().seloFinal(), v.totalGlosa()))
                .toList();
        return new VersoFaturaDetalheResponse(espelho.getIdEspelho(), espelho.getNumeroFatura(),
                espelho.getOcs().getNome(), guias);
    }

    @Transactional(readOnly = true)
    public PdfVerso pdfDaGuia(Long faturaId, Long guiaId) {
        usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        Espelho espelho = faturaLiberada(faturaId);
        VersoGuia verso = versos(espelho).stream()
                .filter(v -> v.guiaId().equals(guiaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia sem verso nesta fatura"));
        return new PdfVerso(versoPdf.gerar(List.of(verso), agora().toLocalDate()),
                "verso-guia-" + verso.guiaNumero() + ".pdf");
    }

    @Transactional(readOnly = true)
    public PdfVerso pdfDaFatura(Long faturaId) {
        usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        Espelho espelho = faturaLiberada(faturaId);
        List<VersoGuia> versos = versos(espelho);
        if (versos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Fatura sem Guia glosada");
        }
        String numero = espelho.getNumeroFatura() == null ? String.valueOf(faturaId)
                : espelho.getNumeroFatura().replaceAll("[^0-9A-Za-z]", "-");
        return new PdfVerso(versoPdf.gerar(versos, agora().toLocalDate()), "versos-fatura-" + numero + ".pdf");
    }

    private Espelho faturaLiberada(Long faturaId) {
        Espelho espelho = espelhoRepository.findById(faturaId)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (!liberada(espelho, agora())) {
            throw new ConflitoException("Os versos ficam disponíveis depois da resposta da OCS ou do fim do prazo.");
        }
        return espelho;
    }

    /** Em auditoria ou auditada; ou aguardando a OCS com ciencia ou prazo vencido do envio mais recente. */
    private boolean liberada(Espelho espelho, LocalDateTime agora) {
        SituacaoFatura situacao = espelho.getSituacaoFatura();
        if (situacao == SituacaoFatura.EM_AUDITORIA || situacao == SituacaoFatura.AUDITADA) {
            return true;
        }
        if (situacao != SituacaoFatura.AGUARDANDO_OCS) {
            return false;
        }
        FaturaEvento envio = null;
        boolean ciencia = false;
        for (FaturaEvento evento : eventoRepository
                .findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(espelho.getIdEspelho())) {
            if (evento.getTipo() == TipoFaturaEvento.ENVIADA_OCS) {
                envio = evento;
                ciencia = false;
            } else if (envio != null && evento.getTipo() == TipoFaturaEvento.CIENTE_OCS) {
                ciencia = true;
            }
        }
        return envio != null && (ciencia || agora.isAfter(FaturaTransicaoService.somarDiasUteis(
                envio.getDataEvento(), FaturaTransicaoService.PRAZO_OCS_DIAS_UTEIS)));
    }

    /** Um verso por Guia que foi a OCS, na ordem do numero da Guia. */
    private List<VersoGuia> versos(Espelho espelho) {
        Long faturaId = espelho.getIdEspelho();
        List<AndamentoGlosaResponse> andamentos = andamentoGlosaService.daFatura(faturaId);
        if (andamentos.isEmpty()) {
            return List.of();
        }
        Set<Long> itensQueForamAOcs = andamentoRepository.rodadasDaFatura(faturaId).stream()
                .map(r -> r.getEspelhoItem().getIdEspelhoItem())
                .collect(Collectors.toSet());
        Map<Long, List<EspelhoItem>> itensPorGuia = espelhoItemRepository.buscarComDadosPorEspelho(faturaId).stream()
                .filter(item -> itensQueForamAOcs.contains(item.getIdEspelhoItem()))
                .sorted(Comparator.comparing(EspelhoItem::getIdEspelhoItem))
                .collect(Collectors.groupingBy(item -> item.getGuiaProcedimento().getGuia().getIdGuia(),
                        LinkedHashMap::new, Collectors.toList()));

        return andamentos.stream()
                .filter(andamento -> itensPorGuia.containsKey(andamento.guiaId()))
                .map(andamento -> verso(espelho, itensPorGuia.get(andamento.guiaId()), andamento))
                .toList();
    }

    private VersoGuia verso(Espelho espelho, List<EspelhoItem> itens, AndamentoGlosaResponse andamento) {
        Guia guia = itens.get(0).getGuiaProcedimento().getGuia();
        PreGuia preGuia = guia.getPreGuia();
        Beneficiario titular = preGuia.getBeneficiario();
        boolean dependente = preGuia.getDependente() != null;

        List<VersoGuia.Linha> linhas = itens.stream()
                .map(item -> new VersoGuia.Linha(
                        item.getGuiaProcedimento().getCodDgp(),
                        item.getGuiaProcedimento().getDescricaoProcedimento(),
                        item.getQuantidadeSessoes(),
                        item.getValorSessao(),
                        item.getValorContratoSessao(),
                        ControleFluxoFatura.glosaDaLinha(item)))
                .toList();
        BigDecimal total = linhas.stream().map(VersoGuia.Linha::glosa).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        List<String> observacoes = itens.stream().map(EspelhoItem::getObservacao)
                .filter(o -> o != null && !o.isBlank()).distinct().toList();

        return new VersoGuia(guia.getIdGuia(), guia.getNumero(), espelho.getNumeroFatura(),
                dependente ? preGuia.getDependente().getNome() : titular.getNome(),
                dependente ? titular.getNome() : null,
                espelho.getOcs().getNome(), espelho.getOcs().getContratoNum(),
                linhas, total, observacoes, andamento);
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now(Beneficiario.FUSO);
    }

    /** PDF gerado e o nome do arquivo. */
    public record PdfVerso(byte[] conteudo, String nomeArquivo) {
    }
}
