package fatec.fusex.nexus.espelho;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import fatec.fusex.nexus.espelho.dto.ValorProcedimentoRequest;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.ContaOcsService;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@Service
public class EspelhoEdicaoService {

    private final ContaOcsService contaOcsService;
    private final UsuarioLogado usuarioLogado;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final TransactionTemplate transacaoEscrita;

    public EspelhoEdicaoService(ContaOcsService contaOcsService,
                                UsuarioLogado usuarioLogado,
                                GuiaRepository guiaRepository,
                                GuiaProcedimentoRepository procedimentoRepository,
                                EspelhoRepository espelhoRepository,
                                EspelhoItemRepository espelhoItemRepository,
                                PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.usuarioLogado = usuarioLogado;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
    }

    public EspelhoResponse editarValores(Long guiaId, List<ValorProcedimentoRequest> valores) {
        usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();

        return transacaoEscrita.execute(status -> editar(guiaId, valores, idOcs));
    }

    private EspelhoResponse editar(Long guiaId, List<ValorProcedimentoRequest> valores, Long idOcs) {
        Guia guia = guiaRepository.findByIdComLock(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia " + guiaId + " nao encontrada"));
        if (!Objects.equals(guia.getPreGuia().getOcs().getIdOcs(), idOcs)) {
            throw new AcessoNegadoException("Guia de outra OCS: so e possivel editar guias da propria OCS");
        }

        Espelho espelho = espelhoRepository
                .findByOcsIdOcsAndStatus(idOcs, StatusEspelho.EM_CONSTRUCAO)
                .orElseThrow(() -> new ConflitoException("Nao ha espelho em construcao para alterar"));
        if (guia.getEspelho() == null
                || !Objects.equals(guia.getEspelho().getIdEspelho(), espelho.getIdEspelho())) {
            throw new RecursoNaoEncontradoException("Guia " + guiaId + " nao esta no espelho em construcao");
        }

        List<GuiaProcedimento> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(guiaId));
        Map<String, BigDecimal> valorPorCodigo = mapearValores(valores, procedimentos);

        espelhoItemRepository.buscarComDadosPorEspelho(espelho.getIdEspelho()).stream()
                .filter(item -> Objects.equals(item.getGuiaProcedimento().getGuia().getIdGuia(), guiaId))
                .forEach(item -> item.setValorSessao(
                        valorPorCodigo.get(item.getGuiaProcedimento().getCodDgp())));

        return EspelhoResponse.fromEntity(
                espelho,
                espelhoItemRepository.buscarComDadosPorEspelho(espelho.getIdEspelho()));
    }

    private Map<String, BigDecimal> mapearValores(List<ValorProcedimentoRequest> valores,
                                                  List<GuiaProcedimento> procedimentos) {
        if (valores == null || valores.size() != procedimentos.size()) {
            throw new RegraDeNegocioException("Informe o valor por sessao de todos os procedimentos da guia");
        }

        Map<String, BigDecimal> porCodigo = new HashMap<>();
        for (ValorProcedimentoRequest valor : valores) {
            if (valor.codDgp() == null
                    || valor.valorSessao() == null
                    || valor.valorSessao().signum() <= 0
                    || valor.valorSessao().scale() > 2) {
                throw new RegraDeNegocioException(
                        "valor_sessao deve ser maior que zero, com no maximo 2 casas decimais");
            }
            if (porCodigo.put(valor.codDgp(), valor.valorSessao()) != null) {
                throw new RegraDeNegocioException("Procedimento " + valor.codDgp() + " informado mais de uma vez");
            }
        }

        for (GuiaProcedimento procedimento : procedimentos) {
            if (!porCodigo.containsKey(procedimento.getCodDgp())) {
                throw new RegraDeNegocioException(
                        "Procedimento " + procedimento.getCodDgp() + " sem valor por sessao");
            }
        }
        return porCodigo;
    }
}