package fatec.fusex.nexus.espelho;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.espelho.dto.EspelhoResponse;
import fatec.fusex.nexus.ocs.ContaOcsService;

@Service
public class EspelhoService {

    private final ContaOcsService contaOcsService;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final TransactionTemplate transacaoLeitura;

    public EspelhoService(ContaOcsService contaOcsService,
                          EspelhoRepository espelhoRepository,
                          EspelhoItemRepository espelhoItemRepository,
                          PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
    }

    public EspelhoResponse consultarEmConstrucao() {
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();

        return transacaoLeitura.execute(status -> espelhoRepository
                .findByOcsIdOcsAndStatus(idOcs, StatusEspelho.EM_CONSTRUCAO)
                .map(this::montar)
                .orElseGet(EspelhoResponse::vazio));
    }

    private EspelhoResponse montar(Espelho espelho) {
        List<EspelhoItem> itens = espelhoItemRepository.buscarComDadosPorEspelho(espelho.getIdEspelho());
        return EspelhoResponse.fromEntity(espelho, itens);
    }
}