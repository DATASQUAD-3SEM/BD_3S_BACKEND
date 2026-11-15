package fatec.fusex.nexus.guiaFinalizada;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guia.ViradaValidadeGuia;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;
import fatec.fusex.nexus.guiaFinalizada.dto.GuiaFinalizadaResponse;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.ContaOcsService;

@Service
public class GuiaFinalizadaService {

    private static final List<SituacaoExecucao> FINALIZADAS =
            List.of(SituacaoExecucao.REALIZADA, SituacaoExecucao.ENCERRADA);

    private final ContaOcsService contaOcsService;
    private final GuiaRepository guiaRepository;
    private final GuiaFinalizadaRepository guiaFinalizadaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final ViradaValidadeGuia viradaValidade;
    private final TransactionTemplate transacaoLeitura;

    public GuiaFinalizadaService(ContaOcsService contaOcsService,
                                 GuiaRepository guiaRepository,
                                 GuiaFinalizadaRepository guiaFinalizadaRepository,
                                 GuiaProcedimentoRepository procedimentoRepository,
                                 ViradaValidadeGuia viradaValidade,
                                 PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.guiaRepository = guiaRepository;
        this.guiaFinalizadaRepository = guiaFinalizadaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.viradaValidade = viradaValidade;
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
    }

    public List<GuiaFinalizadaResponse> listar() {
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();
        LocalDate hoje = Beneficiario.hoje();

        List<Guia> enviadas = transacaoLeitura.execute(status ->
                guiaRepository.findByPreGuiaOcsIdOcsAndDataEnvioIsNotNullOrderByDataEnvioDescIdGuiaDesc(idOcs));
        viradaValidade.virarVencidas(enviadas, hoje);

        return transacaoLeitura.execute(status -> montar(idOcs));
    }

    private List<GuiaFinalizadaResponse> montar(Long idOcs) {
        List<Guia> finalizadas = guiaFinalizadaRepository.buscarFinalizadasDaOcs(idOcs, FINALIZADAS);
        if (finalizadas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = finalizadas.stream().map(Guia::getIdGuia).toList();

        Map<Long, List<ProcedimentoDto>> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        procedimento -> procedimento.getGuia().getIdGuia(),
                        Collectors.mapping(ProcedimentoDto::fromEntity, Collectors.toList())));

        return finalizadas.stream()
                .map(guia -> GuiaFinalizadaResponse.fromEntity(
                        guia, procedimentos.getOrDefault(guia.getIdGuia(), List.of())))
                .toList();
    }
}