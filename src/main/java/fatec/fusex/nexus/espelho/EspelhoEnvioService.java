package fatec.fusex.nexus.espelho;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.espelho.dto.EspelhoResponse;
import fatec.fusex.nexus.ocs.ContaOcsService;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class EspelhoEnvioService {

    private final ContaOcsService contaOcsService;
    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final TransactionTemplate transacaoEscrita;

    public EspelhoEnvioService(ContaOcsService contaOcsService,
                               UsuarioLogado usuarioLogado,
                               EspelhoRepository espelhoRepository,
                               EspelhoItemRepository espelhoItemRepository,
                               PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
    }

    public EspelhoResponse enviar() {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();

        return transacaoEscrita.execute(status -> enviar(usuario, idOcs));
    }

    private EspelhoResponse enviar(Usuario usuario, Long idOcs) {
        Espelho espelho = espelhoRepository
                .buscarComLockPorOcsEStatus(idOcs, StatusEspelho.EM_CONSTRUCAO)
                .orElseThrow(() -> new ConflitoException("Nao ha espelho em construcao para enviar"));

        if (!espelhoItemRepository.existsByEspelhoIdEspelho(espelho.getIdEspelho())) {
            throw new ConflitoException("O espelho em construcao nao tem guias");
        }

        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setDataEnvio(LocalDateTime.now(Beneficiario.FUSO));
        espelho.setEnviadoPor(usuario);

        return EspelhoResponse.fromEntity(
                espelho,
                espelhoItemRepository.buscarComDadosPorEspelho(espelho.getIdEspelho()));
    }
}