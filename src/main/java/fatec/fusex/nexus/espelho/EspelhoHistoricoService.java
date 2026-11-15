package fatec.fusex.nexus.espelho;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.espelho.dto.EspelhoResponse;
import fatec.fusex.nexus.ocs.ContaOcsService;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@Service
public class EspelhoHistoricoService {

    private final ContaOcsService contaOcsService;
    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final TransactionTemplate transacaoLeitura;

    public EspelhoHistoricoService(ContaOcsService contaOcsService,
                                   UsuarioLogado usuarioLogado,
                                   EspelhoRepository espelhoRepository,
                                   EspelhoItemRepository espelhoItemRepository,
                                   PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
    }

    public List<EspelhoResponse> listarEnviados() {
        usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();

        return transacaoLeitura.execute(status -> montar(idOcs));
    }

    private List<EspelhoResponse> montar(Long idOcs) {
        List<Espelho> enviados = espelhoRepository
                .findByOcsIdOcsAndStatusOrderByDataEnvioDescIdEspelhoDesc(idOcs, StatusEspelho.ENVIADO);
        if (enviados.isEmpty()) {
            return List.of();
        }

        List<Long> ids = enviados.stream().map(Espelho::getIdEspelho).toList();
        Map<Long, List<EspelhoItem>> itensPorEspelho = espelhoItemRepository
                .buscarComDadosPorEspelhos(ids)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getEspelho().getIdEspelho()));

        return enviados.stream()
                .map(espelho -> EspelhoResponse.fromEntity(
                        espelho, itensPorEspelho.getOrDefault(espelho.getIdEspelho(), List.of())))
                .toList();
    }
}