package fatec.fusex.nexus.rastreabilidade;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.rastreabilidade.dto.EnviarFaturaRequest;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaConsultaService;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class LisuraAuditoriaService {

    private static final int RESPONSAVEL_MINIMO = 3;
    private static final int RESPONSAVEL_MAXIMO = 100;

    private final UsuarioLogado usuarioLogado;
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;
    private final FaturaEventoRepository eventoRepository;
    private final FaturaTransicaoService transicaoService;
    private final FaturaConsultaService consultaService;

    public LisuraAuditoriaService(UsuarioLogado usuarioLogado,
                                  EspelhoRepository espelhoRepository,
                                  EspelhoItemRepository espelhoItemRepository,
                                  FaturaEventoRepository eventoRepository,
                                  FaturaTransicaoService transicaoService,
                                  FaturaConsultaService consultaService) {
        this.usuarioLogado = usuarioLogado;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
        this.eventoRepository = eventoRepository;
        this.transicaoService = transicaoService;
        this.consultaService = consultaService;
    }

    @Transactional
    public FaturaDetalheResponse enviarAuditoria(Long id, EnviarFaturaRequest request) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.RASTREABILIDADE);
        String responsavel = validarResponsavel(request.responsavel());

        Espelho espelho = espelhoRepository.buscarComLockPorId(id)
                .filter(e -> e.getSituacaoFatura() != null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fatura nao encontrada"));
        LocalDateTime agora = LocalDateTime.now(Beneficiario.FUSO);

        SituacaoFatura situacao = espelho.getSituacaoFatura();
        if (situacao == SituacaoFatura.EM_LISURA) {
            verificarSemGlosaETodasDecididas(id);
        } else if (situacao == SituacaoFatura.AGUARDANDO_OCS) {
            List<FaturaEvento> eventos = eventoRepository
                    .findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(id);
            if (!FaturaTransicaoService.prazoLiberado(eventos, agora)) {
                throw new ConflitoException("Aguardando a decisão da OCS ou o fim do prazo de 3 dias úteis.");
            }
        } else {
            throw new ConflitoException("Esta acao nao e permitida na situacao atual da fatura");
        }

        transicaoService.registrar(id, TipoFaturaEvento.ENVIADA_AUDITORIA, usuario, responsavel);
        return consultaService.montarDetalhe(id);
    }

    private void verificarSemGlosaETodasDecididas(Long id) {
        List<EspelhoItem> itens = espelhoItemRepository.buscarComDadosPorEspelho(id);
        long semDecisao = itens.stream().filter(item -> item.getValorContratoSessao() == null).count();
        if (semDecisao > 0) {
            throw new ConflitoException("Há " + semDecisao + " linha(s) sem decisão.");
        }
        if (itens.stream().anyMatch(item -> ControleFluxoFatura.glosaDaLinha(item).signum() != 0)) {
            throw new ConflitoException("Esta fatura tem glosa; envie para a OCS antes da auditoria.");
        }
    }

    private String validarResponsavel(String responsavel) {
        String nome = responsavel == null ? "" : responsavel.trim();
        if (nome.length() < RESPONSAVEL_MINIMO || nome.length() > RESPONSAVEL_MAXIMO) {
            throw new DadosInvalidosException("responsavel",
                    "Informe o nome do responsável (mínimo de 3 caracteres).");
        }
        return nome;
    }
}