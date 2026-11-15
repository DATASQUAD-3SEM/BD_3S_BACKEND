package fatec.fusex.nexus.guiaBeneficiario;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.ViradaValidadeGuia;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.SessaoDto;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * T18-BE1: regras de GET /guias/minhas.
 *
 * Ao listar, aplica a virada de validade (ViradaValidadeGuia) nas guias do beneficiario; a leitura final
 * roda em transacao somente leitura.
 */
@Service
public class GuiaBeneficiarioService {

    private final UsuarioLogado usuarioLogado;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final ViradaValidadeGuia viradaValidade;
    private final TransactionTemplate transacaoLeitura;

    public GuiaBeneficiarioService(UsuarioLogado usuarioLogado,
                                   GuiaRepository guiaRepository,
                                   GuiaProcedimentoRepository procedimentoRepository,
                                   SessaoRepository sessaoRepository,
                                   ViradaValidadeGuia viradaValidade,
                                   PlatformTransactionManager transactionManager) {
        this.usuarioLogado = usuarioLogado;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.viradaValidade = viradaValidade;
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
    }

    public List<GuiaBeneficiarioResponse> listar() {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO);
        Long usuarioId = usuario.getIdUsuario();
        LocalDate hoje = Beneficiario.hoje();

        List<Guia> guias = transacaoLeitura.execute(status ->
                guiaRepository.findByPreGuiaBeneficiarioUsuarioIdOrderByDataInsercaoDescIdGuiaDesc(usuarioId));
        viradaValidade.virarVencidas(guias, hoje);

        return transacaoLeitura.execute(status -> montar(usuarioId));
    }

    private List<GuiaBeneficiarioResponse> montar(Long usuarioId) {
        List<Guia> guias = guiaRepository.findByPreGuiaBeneficiarioUsuarioIdOrderByDataInsercaoDescIdGuiaDesc(usuarioId);
        if (guias.isEmpty()) {
            return List.of();
        }
        List<Long> ids = guias.stream().map(Guia::getIdGuia).toList();

        Map<Long, List<ProcedimentoDto>> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        procedimento -> procedimento.getGuia().getIdGuia(),
                        Collectors.mapping(ProcedimentoDto::fromEntity, Collectors.toList())));

        Map<Long, List<SessaoDto>> sessoes = sessaoRepository
                .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        sessao -> sessao.getGuiaProcedimento().getGuia().getIdGuia(),
                        Collectors.mapping(SessaoDto::fromEntity, Collectors.toList())));

        return guias.stream()
                .map(guia -> GuiaBeneficiarioResponse.fromEntity(
                        guia,
                        procedimentos.getOrDefault(guia.getIdGuia(), List.of()),
                        sessoes.getOrDefault(guia.getIdGuia(), List.of())))
                .toList();
    }
}
