package fatec.fusex.nexus.espelho;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.espelho.dto.EspelhoResponse;
import fatec.fusex.nexus.espelho.dto.ValorProcedimentoRequest;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.ContaOcsService;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class EspelhoGuiaService {

    private static final Set<SituacaoExecucao> FINALIZADAS =
            Set.of(SituacaoExecucao.REALIZADA, SituacaoExecucao.ENCERRADA);

    private final ContaOcsService contaOcsService;
    private final UsuarioLogado usuarioLogado;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final TransactionTemplate transacaoEscrita;

    public EspelhoGuiaService(ContaOcsService contaOcsService,
                              UsuarioLogado usuarioLogado,
                              GuiaRepository guiaRepository,
                              GuiaProcedimentoRepository procedimentoRepository,
                              SessaoRepository sessaoRepository,
                              EspelhoRepository espelhoRepository,
                              EspelhoItemRepository espelhoItemRepository,
                              PlatformTransactionManager transactionManager) {
        this.contaOcsService = contaOcsService;
        this.usuarioLogado = usuarioLogado;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
    }

    public EspelhoResponse adicionarGuia(Long guiaId, List<ValorProcedimentoRequest> valores) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        Long idOcs = contaOcsService.ocsDaContaLogada().getIdOcs();

        return transacaoEscrita.execute(status -> adicionar(guiaId, valores, usuario, idOcs));
    }

    private EspelhoResponse adicionar(Long guiaId,
                                      List<ValorProcedimentoRequest> valores,
                                      Usuario usuario,
                                      Long idOcs) {
        Guia guia = guiaRepository.findByIdComLock(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia " + guiaId + " nao encontrada"));

        Ocs ocsDaGuia = guia.getPreGuia().getOcs();
        if (!Objects.equals(ocsDaGuia.getIdOcs(), idOcs)) {
            throw new AcessoNegadoException("Guia de outra OCS: so e possivel adicionar guias da propria OCS");
        }
        if (!FINALIZADAS.contains(guia.getSituacaoExecucao())) {
            throw new ConflitoException("Guia nao esta realizada nem encerrada");
        }
        if (guia.getEspelho() != null) {
            throw new ConflitoException("Guia ja vinculada a um espelho");
        }

        List<GuiaProcedimento> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(guiaId));
        Map<String, BigDecimal> valorPorCodigo = mapearValores(valores, procedimentos);
        Map<Long, Long> assinadasPorProcedimento = contarAssinadas(guiaId);

        Espelho espelho = espelhoRepository
                .findByOcsIdOcsAndStatus(idOcs, StatusEspelho.EM_CONSTRUCAO)
                .orElseGet(() -> novoEspelho(ocsDaGuia, usuario));

        for (GuiaProcedimento procedimento : procedimentos) {
            int assinadas = assinadasPorProcedimento
                    .getOrDefault(procedimento.getIdGuiaProcedimento(), 0L)
                    .intValue();
            if (assinadas == 0) {
                throw new RegraDeNegocioException(
                        "Procedimento " + procedimento.getCodDgp() + " nao tem sessao assinada");
            }

            EspelhoItem item = new EspelhoItem();
            item.setEspelho(espelho);
            item.setGuiaProcedimento(procedimento);
            item.setQuantidadeSessoes(assinadas);
            item.setValorSessao(valorPorCodigo.get(procedimento.getCodDgp()));
            espelhoItemRepository.save(item);
        }

        guia.setEspelho(espelho);

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

    private Map<Long, Long> contarAssinadas(Long guiaId) {
        return sessaoRepository
                .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                        List.of(guiaId))
                .stream()
                .filter(sessao -> sessao.getStatus() == SituacaoSessao.ASSINADA)
                .collect(Collectors.groupingBy(
                        sessao -> sessao.getGuiaProcedimento().getIdGuiaProcedimento(),
                        Collectors.counting()));
    }

    private Espelho novoEspelho(Ocs ocs, Usuario usuario) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.EM_CONSTRUCAO);
        espelho.setDataCriacao(LocalDateTime.now(Beneficiario.FUSO));
        espelho.setCriadoPor(usuario);
        return espelhoRepository.save(espelho);
    }
}