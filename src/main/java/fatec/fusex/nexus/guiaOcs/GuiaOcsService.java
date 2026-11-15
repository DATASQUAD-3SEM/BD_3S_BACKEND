package fatec.fusex.nexus.guiaOcs;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guia.ViradaValidadeGuia;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.SessaoDto;
import fatec.fusex.nexus.guiaOcs.dto.GuiaOcsDetalheResponse;
import fatec.fusex.nexus.guiaOcs.dto.GuiaOcsResponse;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.ContaOcsService;
import fatec.fusex.nexus.sessao.SessaoRepository;

/**
 * T19-BE1 e T19-BE2: regras de GET /ocs/guias e GET /ocs/guias/{id}.
 *
 * A OCS ve as guias enviadas a ela (via ContaOcs). Antes de devolver, aplica a virada de validade (T18-BE1);
 * so depois exclui NAO_ENVIADA e VENCIDA, porque a virada pode transformar uma guia em VENCIDA.
 */
@Service
public class GuiaOcsService {

    // Situacoes que a OCS nao enxerga: sem envio ou vencida sem nenhuma sessao assinada (regra 9).
    private static final Set<SituacaoExecucao> OCULTAS =
            Set.of(SituacaoExecucao.NAO_ENVIADA, SituacaoExecucao.VENCIDA);

    private final ContaOcsService contaOcsService;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final ViradaValidadeGuia viradaValidade;
    private final TransactionTemplate transacaoLeitura;

    public GuiaOcsService(ContaOcsService contaOcsService,
                          GuiaRepository guiaRepository,
                          GuiaProcedimentoRepository procedimentoRepository,
                          SessaoRepository sessaoRepository,
                          ViradaValidadeGuia viradaValidade,
                          PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.viradaValidade = viradaValidade;
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
    }

    public List<GuiaOcsResponse> listar() {
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();
        LocalDate hoje = Beneficiario.hoje();

        List<Guia> guias = transacaoLeitura.execute(status -> buscarEnviadas(idOcs));
        viradaValidade.virarVencidas(guias, hoje);

        return transacaoLeitura.execute(status -> montar(idOcs));
    }

    /** T19-BE2: modelo digitalizado de uma guia. 404 se nao for da OCS, nao enviada ou vencida. */
    public GuiaOcsDetalheResponse detalhar(Long idGuia) {
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();
        LocalDate hoje = Beneficiario.hoje();

        Guia guia = transacaoLeitura.execute(status -> buscarDaOcs(idGuia, idOcs))
                .orElseThrow(() -> naoEncontrada(idGuia));
        viradaValidade.virarVencidas(List.of(guia), hoje);

        return transacaoLeitura.execute(status -> buscarDaOcs(idGuia, idOcs)
                .filter(atual -> !OCULTAS.contains(atual.getSituacaoExecucao()))
                .map(this::montarDetalhe)
                .orElseThrow(() -> naoEncontrada(idGuia)));
    }

    private List<Guia> buscarEnviadas(Long idOcs) {
        return guiaRepository.findByPreGuiaOcsIdOcsAndDataEnvioIsNotNullOrderByDataEnvioDescIdGuiaDesc(idOcs);
    }

    private Optional<Guia> buscarDaOcs(Long idGuia, Long idOcs) {
        return guiaRepository.findByIdGuiaAndPreGuiaOcsIdOcsAndDataEnvioIsNotNull(idGuia, idOcs);
    }

    private static RecursoNaoEncontradoException naoEncontrada(Long idGuia) {
        return new RecursoNaoEncontradoException("Guia " + idGuia + " nao encontrada para esta OCS");
    }

    private List<GuiaOcsResponse> montar(Long idOcs) {
        List<Guia> visiveis = buscarEnviadas(idOcs).stream()
                .filter(guia -> !OCULTAS.contains(guia.getSituacaoExecucao()))
                .toList();
        if (visiveis.isEmpty()) {
            return List.of();
        }
        List<Long> ids = visiveis.stream().map(Guia::getIdGuia).toList();

        Map<Long, List<ProcedimentoDto>> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        procedimento -> procedimento.getGuia().getIdGuia(),
                        Collectors.mapping(ProcedimentoDto::fromEntity, Collectors.toList())));

        return visiveis.stream()
                .map(guia -> GuiaOcsResponse.fromEntity(
                        guia, procedimentos.getOrDefault(guia.getIdGuia(), List.of())))
                .toList();
    }

    private GuiaOcsDetalheResponse montarDetalhe(Guia guia) {
        List<ProcedimentoDto> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(guia.getIdGuia())).stream()
                .map(ProcedimentoDto::fromEntity)
                .toList();

        List<SessaoDto> sessoes = sessaoRepository
                .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                        List.of(guia.getIdGuia())).stream()
                .map(SessaoDto::fromEntity)
                .toList();

        return GuiaOcsDetalheResponse.fromEntity(guia, procedimentos, sessoes);
    }
}
