package fatec.fusex.nexus.setorFatura;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.OrigemContrato;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.ocsPreco.OcsPreco;
import fatec.fusex.nexus.ocsPreco.OcsPrecoRepository;
import fatec.fusex.nexus.setorFatura.dto.EnviarLisuraRequest;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class FaturaEnvioLisuraService {

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final OcsPrecoRepository ocsPrecoRepository;
    private final FaturaTransicaoService transicaoService;
    private final FaturaConsultaService consultaService;

    public FaturaEnvioLisuraService(UsuarioLogado usuarioLogado,
                                    EspelhoRepository espelhoRepository,
                                    EspelhoItemRepository espelhoItemRepository,
                                    OcsPrecoRepository ocsPrecoRepository,
                                    FaturaTransicaoService transicaoService,
                                    FaturaConsultaService consultaService) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.ocsPrecoRepository = ocsPrecoRepository;
        this.transicaoService = transicaoService;
        this.consultaService = consultaService;
    }

    @Transactional
    public FaturaDetalheResponse enviarLisura(Long id, EnviarLisuraRequest request) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_FATURA);

        Espelho espelho = espelhoRepository.buscarComLockPorId(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        if (espelho.getSituacaoFatura() != SituacaoFatura.PROTOCOLADA) {
            throw new ConflitoException("Esta acao nao e permitida na situacao atual da fatura");
        }

        copiarPrecosContrato(espelho);

        transicaoService.registrar(id, TipoFaturaEvento.ENVIADA_LISURA, usuario, request.responsavel());

        return consultaService.detalhar(id);
    }

    private void copiarPrecosContrato(Espelho espelho) {
        List<EspelhoItem> itens = espelhoItemRepository.buscarComDadosPorEspelho(espelho.getIdEspelho());
        for (EspelhoItem item : itens) {
            String codDgp = item.getGuiaProcedimento().getCodDgp();
            Optional<OcsPreco> preco = ocsPrecoRepository
                    .findByOcsIdOcsAndCodDgp(espelho.getOcs().getIdOcs(), codDgp);
            if (preco.isPresent()) {
                item.setValorContratoSessao(preco.get().getValorSessao());
                item.setOrigemContrato(OrigemContrato.TABELA);
            } else {
                item.setValorContratoSessao(null);
                item.setOrigemContrato(null);
            }
            espelhoItemRepository.save(item);
        }
    }
}