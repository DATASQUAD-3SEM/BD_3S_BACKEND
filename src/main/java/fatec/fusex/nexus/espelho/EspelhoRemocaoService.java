package fatec.fusex.nexus.espelho;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.espelho.dto.EspelhoResponse;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.ocs.ContaOcsService;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@Service
public class EspelhoRemocaoService {

    private final ContaOcsService contaOcsService;
    private final UsuarioLogado usuarioLogado;
    private final GuiaRepository guiaRepository;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final TransactionTemplate transacaoEscrita;

    public EspelhoRemocaoService(ContaOcsService contaOcsService,
                                 UsuarioLogado usuarioLogado,
                                 GuiaRepository guiaRepository,
                                 EspelhoRepository espelhoRepository,
                                 EspelhoItemRepository espelhoItemRepository,
                                 PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.usuarioLogado = usuarioLogado;
        this.guiaRepository = guiaRepository;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
    }

    public EspelhoResponse removerGuias(List<Long> guiaIdsInformados) {
        usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();

        return transacaoEscrita.execute(status -> remover(guiaIdsInformados, idOcs));
    }

    private EspelhoResponse remover(List<Long> guiaIdsInformados, Long idOcs) {
        Espelho espelho = espelhoRepository
                .findByOcsIdOcsAndStatus(idOcs, StatusEspelho.EM_CONSTRUCAO)
                .orElseThrow(() -> new ConflitoException("Nao ha espelho em construcao para alterar"));

        List<Long> guiaIds = guiaIdsInformados == null
                ? List.of()
                : guiaIdsInformados.stream().distinct().toList();
        if (guiaIds.isEmpty()) {
            throw new RegraDeNegocioException("Informe ao menos uma guia para remover");
        }

        List<Guia> guias = guiaIds.stream()
                .map(guiaId -> validarGuia(guiaId, espelho, idOcs))
                .toList();

        espelhoItemRepository.deleteAll(
                espelhoItemRepository.findByEspelhoIdEspelhoAndGuiaProcedimentoGuiaIdGuiaIn(
                        espelho.getIdEspelho(), guiaIds));
        guias.forEach(guia -> guia.setEspelho(null));
        espelhoRepository.flush();

        if (!espelhoItemRepository.existsByEspelhoIdEspelho(espelho.getIdEspelho())) {
            espelhoRepository.delete(espelho);
            espelhoRepository.flush();
            return EspelhoResponse.vazio();
        }

        return EspelhoResponse.fromEntity(
                espelho,
                espelhoItemRepository.buscarComDadosPorEspelho(espelho.getIdEspelho()));
    }

    private Guia validarGuia(Long guiaId, Espelho espelho, Long idOcs) {
        Guia guia = guiaRepository.findByIdComLock(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia " + guiaId + " nao encontrada"));

        if (!Objects.equals(guia.getPreGuia().getOcs().getIdOcs(), idOcs)) {
            throw new AcessoNegadoException("Guia de outra OCS: so e possivel remover guias da propria OCS");
        }
        if (guia.getEspelho() == null
                || !Objects.equals(guia.getEspelho().getIdEspelho(), espelho.getIdEspelho())) {
            throw new RecursoNaoEncontradoException("Guia " + guiaId + " nao esta no espelho em construcao");
        }
        return guia;
    }
}