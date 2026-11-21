package fatec.fusex.nexus.rastreabilidade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcs;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcsRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.OrigemContrato;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocsPreco.OcsPreco;
import fatec.fusex.nexus.ocsPreco.OcsPrecoRepository;
import fatec.fusex.nexus.rastreabilidade.dto.EnviarFaturaRequest;
import fatec.fusex.nexus.rastreabilidade.dto.ItemLisuraRequest;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaConsultaService;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class LisuraService {

    private static final int OBSERVACAO_MAXIMO = 500;
    private static final int RESPONSAVEL_MINIMO = 3;
    private static final int RESPONSAVEL_MAXIMO = 100;

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final OcsPrecoRepository ocsPrecoRepository;
    private final EspelhoItemRodadaOcsRepository espelhoItemRodadaOcsRepository;
    private final FaturaConsultaService consultaService;
    private final FaturaTransicaoService transicaoService;

    public LisuraService(UsuarioLogado usuarioLogado,
                         EspelhoRepository espelhoRepository,
                         EspelhoItemRepository espelhoItemRepository,
                         OcsPrecoRepository ocsPrecoRepository,
                         EspelhoItemRodadaOcsRepository espelhoItemRodadaOcsRepository,
                         FaturaConsultaService consultaService,
                         FaturaTransicaoService transicaoService) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.ocsPrecoRepository = ocsPrecoRepository;
        this.espelhoItemRodadaOcsRepository = espelhoItemRodadaOcsRepository;
        this.consultaService = consultaService;
        this.transicaoService = transicaoService;
    }

    /** Glosa pendente: glosa diferente de zero e a OCS ainda nao deu "Ok" na linha. */
    private boolean glosaPendente(EspelhoItem item) {
        return ControleFluxoFatura.glosaDaLinha(item).signum() != 0 && !"OK".equals(item.getDecisaoOcs());
    }

    @Transactional
    public FaturaDetalheResponse salvar(Long id, String responsavelInformado, List<ItemLisuraRequest> itens) {
        usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        Espelho espelho = buscarEmLisura(id);

        Map<Long, EspelhoItem> itensDaFatura = espelhoItemRepository.buscarComDadosPorEspelho(id).stream()
                .collect(Collectors.toMap(EspelhoItem::getIdEspelhoItem, Function.identity()));
        validar(itens, itensDaFatura);

        // Gravar preco na tabela da OCS exige o nome do responsavel (regra 12.3).
        boolean gravaPreco = itens.stream()
                .anyMatch(pedido -> digitaValor(itensDaFatura.get(pedido.itemId()), pedido));
        String responsavelPreco = gravaPreco ? validarResponsavel(responsavelInformado) : null;

        LocalDateTime agora = LocalDateTime.now(Beneficiario.FUSO);
        for (ItemLisuraRequest pedido : itens) {
            aplicar(espelho.getOcs(), itensDaFatura.get(pedido.itemId()), pedido, responsavelPreco, agora);
        }

        return consultaService.montarDetalhe(id);
    }

    @Transactional
    public FaturaDetalheResponse enviarOcs(Long id, EnviarFaturaRequest request) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        String responsavel = validarResponsavel(request.responsavel());
        buscarEmLisura(id);

        List<EspelhoItem> itens = espelhoItemRepository.buscarComDadosPorEspelho(id);
        long semDecisao = itens.stream().filter(item -> item.getValorContratoSessao() == null).count();
        if (semDecisao > 0) {
            throw new DadosInvalidosException("linhas", "Há " + semDecisao + " linha(s) sem decisão.");
        }
        boolean temGlosaPendente = itens.stream().anyMatch(this::glosaPendente);
        if (!temGlosaPendente) {
            throw new ConflitoException("Esta fatura não tem glosa; envie direto para a auditoria.");
        }

        FaturaEvento envio = transicaoService.registrar(id, TipoFaturaEvento.ENVIADA_OCS, usuario, responsavel);
        for (EspelhoItem item : itens) {
            if (!"OK".equals(item.getDecisaoOcs())) {
                // Linha que entra na nova rodada: a decisao anterior (contestacao) e apagada.
                item.setDecisaoOcs(null);
                item.setMotivoContestacaoOcs(null);
            }
            EspelhoItemRodadaOcs rodada = new EspelhoItemRodadaOcs();
            rodada.setEspelhoItem(item);
            rodada.setEnvioEvento(envio);
            rodada.setValorContratoSessao(item.getValorContratoSessao());
            rodada.setOrigemContrato(item.getOrigemContrato());
            espelhoItemRodadaOcsRepository.save(rodada);
        }
        return consultaService.montarDetalhe(id);
    }

    private Espelho buscarEmLisura(Long id) {
        Espelho espelho = espelhoRepository.buscarComLockPorId(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (espelho.getSituacaoFatura() != SituacaoFatura.EM_LISURA) {
            throw new ConflitoException("Esta acao nao e permitida na situacao atual da fatura");
        }
        return espelho;
    }

    private String validarResponsavel(String responsavel) {
        String nome = responsavel == null ? "" : responsavel.trim();
        if (nome.length() < RESPONSAVEL_MINIMO || nome.length() > RESPONSAVEL_MAXIMO) {
            throw new DadosInvalidosException("responsavel",
                    "Informe o nome do responsável (mínimo de 3 caracteres).");
        }
        return nome;
    }

    private void validar(List<ItemLisuraRequest> itens, Map<Long, EspelhoItem> itensDaFatura) {
        for (ItemLisuraRequest pedido : itens) {
            if (pedido.itemId() == null || !itensDaFatura.containsKey(pedido.itemId())) {
                throw new RegraDeNegocioException("Item nao pertence a esta fatura");
            }
            if (pedido.observacao() != null && pedido.observacao().length() > OBSERVACAO_MAXIMO) {
                throw new RegraDeNegocioException("A observacao aceita no maximo 500 caracteres");
            }
            EspelhoItem item = itensDaFatura.get(pedido.itemId());
            if (digitaValor(item, pedido)) {
                BigDecimal valor = pedido.valorContrato();
                if (valor == null) {
                    throw new RegraDeNegocioException("Informe o valor do contrato");
                }
                if (valor.signum() < 0) {
                    throw new RegraDeNegocioException("O valor do contrato nao pode ser negativo");
                }
            }
        }
    }

    private void aplicar(Ocs ocs, EspelhoItem item, ItemLisuraRequest pedido, String responsavelPreco, LocalDateTime agora) {
        item.setObservacao(normalizar(pedido.observacao()));
        if (digitaValor(item, pedido)) {
            BigDecimal valor = pedido.valorContrato();
            item.setValorContratoSessao(valor);
            if (daTabela(item)) {
                item.setOrigemContrato(OrigemContrato.TABELA_CORRIGIDA);
            } else {
                item.setOrigemContrato(OrigemContrato.MANUAL);
            }
            salvarPreco(ocs, item.getGuiaProcedimento().getCodDgp(), valor, responsavelPreco, agora);
        } else if (!daTabela(item)) {
            if (Boolean.TRUE.equals(pedido.conforme())) {
                item.setValorContratoSessao(item.getValorSessao());
                item.setOrigemContrato(OrigemContrato.CONFERIDO);
            } else {
                item.setValorContratoSessao(null);
                item.setOrigemContrato(null);
            }
        }
    }

    private boolean digitaValor(EspelhoItem item, ItemLisuraRequest pedido) {
        if (daTabela(item)) {
            return Boolean.TRUE.equals(pedido.corrigirTabela());
        }
        return Boolean.FALSE.equals(pedido.conforme()) && pedido.valorContrato() != null;
    }

    private boolean daTabela(EspelhoItem item) {
        return item.getOrigemContrato() == OrigemContrato.TABELA
                || item.getOrigemContrato() == OrigemContrato.TABELA_CORRIGIDA;
    }

    private void salvarPreco(Ocs ocs, String codDgp, BigDecimal valor, String responsavelPreco, LocalDateTime agora) {
        OcsPreco preco = ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), codDgp)
                .orElseGet(OcsPreco::new);
        preco.setOcs(ocs);
        preco.setCodDgp(codDgp);
        preco.setValorSessao(valor);
        preco.setAtualizadoEm(agora);
        preco.setAtualizadoPor(responsavelPreco);
        ocsPrecoRepository.save(preco);
    }

    private String normalizar(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }
        return observacao.trim();
    }
}