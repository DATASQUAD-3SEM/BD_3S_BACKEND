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
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.OrigemContrato;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocsPreco.OcsPreco;
import fatec.fusex.nexus.ocsPreco.OcsPrecoRepository;
import fatec.fusex.nexus.rastreabilidade.dto.ItemLisuraRequest;
import fatec.fusex.nexus.setorFatura.FaturaConsultaService;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class LisuraService {

    private static final int OBSERVACAO_MAXIMO = 500;

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final OcsPrecoRepository ocsPrecoRepository;
    private final FaturaConsultaService consultaService;

    public LisuraService(UsuarioLogado usuarioLogado,
                         EspelhoRepository espelhoRepository,
                         EspelhoItemRepository espelhoItemRepository,
                         OcsPrecoRepository ocsPrecoRepository,
                         FaturaConsultaService consultaService) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.ocsPrecoRepository = ocsPrecoRepository;
        this.consultaService = consultaService;
    }

    @Transactional
    public FaturaDetalheResponse salvar(Long id, List<ItemLisuraRequest> itens) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        Espelho espelho = espelhoRepository.buscarComLockPorId(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (espelho.getSituacaoFatura() != SituacaoFatura.EM_LISURA) {
            throw new ConflitoException("Esta acao nao e permitida na situacao atual da fatura");
        }

        Map<Long, EspelhoItem> itensDaFatura = espelhoItemRepository.buscarComDadosPorEspelho(id).stream()
                .collect(Collectors.toMap(EspelhoItem::getIdEspelhoItem, Function.identity()));
        validar(itens, itensDaFatura);

        LocalDateTime agora = LocalDateTime.now(Beneficiario.FUSO);
        for (ItemLisuraRequest pedido : itens) {
            aplicar(espelho.getOcs(), itensDaFatura.get(pedido.itemId()), pedido, usuario, agora);
        }

        return consultaService.montarDetalhe(id);
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

    private void aplicar(Ocs ocs, EspelhoItem item, ItemLisuraRequest pedido, Usuario usuario, LocalDateTime agora) {
        item.setObservacao(normalizar(pedido.observacao()));
        if (digitaValor(item, pedido)) {
            BigDecimal valor = pedido.valorContrato();
            item.setValorContratoSessao(valor);
            if (daTabela(item)) {
                item.setOrigemContrato(OrigemContrato.TABELA_CORRIGIDA);
            } else {
                item.setOrigemContrato(OrigemContrato.MANUAL);
            }
            salvarPreco(ocs, item.getGuiaProcedimento().getCodDgp(), valor, usuario, agora);
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

    private void salvarPreco(Ocs ocs, String codDgp, BigDecimal valor, Usuario usuario, LocalDateTime agora) {
        OcsPreco preco = ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), codDgp)
                .orElseGet(OcsPreco::new);
        preco.setOcs(ocs);
        preco.setCodDgp(codDgp);
        preco.setValorSessao(valor);
        preco.setAtualizadoEm(agora);
        preco.setAtualizadoPor(usuario);
        ocsPrecoRepository.save(preco);
    }

    private String normalizar(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }
        return observacao.trim();
    }
}