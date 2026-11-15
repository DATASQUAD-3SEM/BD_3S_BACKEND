package fatec.fusex.nexus.guiaBeneficiario;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.ViradaValidadeGuia;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.SessaoDto;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * T18-BE1: regras de GET /guias/minhas.
 * T18-BE-2: envio da guia à OCS, criando uma sessão por unidade autorizada.
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

    // Ordem: perfil (403) -> existe (404) -> dono (403) -> situacao (409) -> validade (409) -> exames (409).
    // Trava a Guia (findByIdComLock) para que dois envios simultaneos nao criem sessao em dobro.

    @Transactional(rollbackFor = Exception.class) // CORRIGIDO: Adicionado o @
    public GuiaBeneficiarioResponse enviar(Long idGuia) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO);

        Guia guia = guiaRepository.findByIdComLock(idGuia)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + idGuia));

        if (!guia.getPreGuia().getBeneficiario().getUsuarioId().equals(usuario.getIdUsuario())) {
            throw new AcessoNegadoException("A Guia nao pertence ao usuario logado");
        }

        if (guia.getSituacaoExecucao() != SituacaoExecucao.NAO_ENVIADA) {
            throw new ConflitoException("Esta guia ja foi enviada à OCS");
        }

        if (guia.getDataValidadeAte() == null) {
            throw new ConflitoException("Guia sem validade");
        }

        List<GuiaProcedimento> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(idGuia));
        if (procedimentos.isEmpty()) {
            throw new ConflitoException("Guia sem exames cadastrados");
        }

        guia.setDataEnvio(LocalDateTime.now());
        guia.setEnviadaPor(usuario);
        guia.setSituacaoExecucao(SituacaoExecucao.ENVIADA);

        // Uma Sessao por unidade de quantidade_autorizada, numeradas de 1 a N, todas DISPONIVEL.
        // A OCS vem de PreGuia.ocs_id: nao ha OCS no corpo da requisicao.
        List<Sessao> sessoes = new ArrayList<>();
        for (GuiaProcedimento procedimento : procedimentos) {
            for (int numero = 1; numero <= procedimento.getQuantidadeAutorizada(); numero++) {
                Sessao sessao = new Sessao();
                sessao.setGuiaProcedimento(procedimento);
                sessao.setNumero(numero);
                sessao.setStatus(SituacaoSessao.DISPONIVEL);
                sessoes.add(sessao);
            }
        }
        sessaoRepository.saveAll(sessoes);

        // Devolve a guia atualizada, no mesmo formato do T18-BE-1.
        List<ProcedimentoDto> procedimentosDto = procedimentos.stream()
                .map(ProcedimentoDto::fromEntity)
                .toList();
        List<SessaoDto> sessoesDto = sessoes.stream()
                .map(SessaoDto::fromEntity)
                .toList();

        return GuiaBeneficiarioResponse.fromEntity(guia, procedimentosDto, sessoesDto);
    } 
}