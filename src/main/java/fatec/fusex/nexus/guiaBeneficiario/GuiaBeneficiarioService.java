package fatec.fusex.nexus.guiaBeneficiario;

import fatec.fusex.nexus.preGuia.AcessoPaciente;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuia.dto.PacienteDto;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guia.ViradaValidadeGuia;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * T18-BE1: regras de GET /guias/minhas.
 * T18-BE-2: envio da guia à OCS, criando uma sessão por unidade autorizada.
 * T18-BE-3: assinatura de uma sessão SOLICITADA, uma por vez, com regra 25 e lock pessimista.
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
    private final TransactionTemplate transacaoEscrita;

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
        this.transacaoEscrita = new TransactionTemplate(transactionManager);
    }

    public List<GuiaBeneficiarioResponse> listar() {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO);
        LocalDate hoje = Beneficiario.hoje();

        List<Guia> guias = transacaoLeitura.execute(status -> guiasQuePodeAgir(usuario));
        viradaValidade.virarVencidas(guias, hoje);

        return transacaoLeitura.execute(status -> montar(guiasQuePodeAgir(usuario), usuario));
    }

    /**
     * T30-BE3: guias que o usuario pode ver e agir (AcessoPaciente). O titular nao ve as guias do dependente
     * maior nem as do menor sob responsavel legal; o responsavel ve as dos menores que acompanha; o maior ve as
     * proprias. T18-BE1: so as guias ja assinadas pelo medico e pelo chefe (pre-guia CONCLUIDA); a Guia recem-inserida
     * pelo setor de Guias, ainda sem as duas assinaturas, nao aparece (regras 2, 4 e 6.2). Ordem: mais recente primeiro.
     */
    private List<Guia> guiasQuePodeAgir(Usuario usuario) {
        Map<Long, Guia> porId = new java.util.LinkedHashMap<>();
        for (Guia guia : guiaRepository.findByPreGuiaBeneficiarioUsuarioIdOrderByDataInsercaoDescIdGuiaDesc(usuario.getIdUsuario())) {
            porId.put(guia.getIdGuia(), guia);
        }
        for (Guia guia : guiaRepository.findByPreGuiaDependenteResponsavelIdUsuarioOrderByDataInsercaoDescIdGuiaDesc(usuario.getIdUsuario())) {
            porId.put(guia.getIdGuia(), guia);
        }
        for (Guia guia : guiaRepository.findByPreGuiaDependenteUsuarioIdUsuarioOrderByDataInsercaoDescIdGuiaDesc(usuario.getIdUsuario())) {
            porId.put(guia.getIdGuia(), guia);
        }
        return porId.values().stream()
                .filter(guia -> AcessoPaciente.podeAgir(usuario, guia.getPreGuia()))
                .filter(guia -> guia.getPreGuia().getStatus() == PreGuiaStatus.CONCLUIDA)
                .sorted(java.util.Comparator.comparing(Guia::getDataInsercao).reversed()
                        .thenComparing(Guia::getIdGuia, java.util.Comparator.reverseOrder()))
                .toList();
    }

    private List<GuiaBeneficiarioResponse> montar(List<Guia> guias, Usuario usuario) {
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
                        sessoes.getOrDefault(guia.getIdGuia(), List.of()),
                        PacienteDto.de(guia.getPreGuia(), usuario)))
                .toList();
    }

    // Ordem: perfil (403) -> existe (404) -> dono (403) -> pre-guia CONCLUIDA (409) -> situacao (409)
    //     -> validade (409) -> exames (409).
    // Trava a Guia (findByIdComLock) para que dois envios simultaneos nao criem sessao em dobro.

    @Transactional(rollbackFor = Exception.class)
    public GuiaBeneficiarioResponse enviar(Long idGuia) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO);

        Guia guia = guiaRepository.findByIdComLock(idGuia)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + idGuia));

        if (!AcessoPaciente.podeAgir(usuario, guia.getPreGuia())) {
            throw new AcessoNegadoException("A Guia nao pertence ao usuario logado");
        }

        // T18-BE2: so a Guia assinada pelo medico e pelo chefe (pre-guia CONCLUIDA) pode ser usada (regra 6.2).
        if (guia.getPreGuia().getStatus() != PreGuiaStatus.CONCLUIDA) {
            throw new ConflitoException("A Guia ainda não foi assinada pelo médico e pelo chefe do FUSEX");
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

        return GuiaBeneficiarioResponse.fromEntity(guia, procedimentosDto, sessoesDto,
                PacienteDto.de(guia.getPreGuia(), usuario));
    }

    // Ordem: perfil (403) -> guia existe (404) -> dono (403) -> validade (409) -> sessao da guia (404)
    //     -> status SOLICITADA (409) -> assinatura cadastrada (422) -> regra 25 (409).
    // T18-BE3 (regra 8.2.1): antes de assinar, aplica a virada de validade numa transacao propria, para que a Guia
    // vencida vire Encerrada ou Vencida e as sessoes solicitadas virem Expiradas mesmo quando a assinatura e recusada.
    public GuiaBeneficiarioResponse assinar(Long idGuia, Long idSessao) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO);
        LocalDate hoje = Beneficiario.hoje();

        transacaoLeitura.execute(status -> guiaRepository.findById(idGuia))
                .ifPresent(guia -> viradaValidade.virarVencidas(List.of(guia), hoje));

        return transacaoEscrita.execute(status -> assinarComLock(usuario, idGuia, idSessao, hoje));
    }

    private GuiaBeneficiarioResponse assinarComLock(Usuario usuario, Long idGuia, Long idSessao, LocalDate hoje) {
        Guia guia = guiaRepository.findByIdComLock(idGuia)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + idGuia));

        if (!AcessoPaciente.podeAgir(usuario, guia.getPreGuia())) {
            throw new AcessoNegadoException("A Guia nao pertence ao usuario logado");
        }

        if (guia.getDataValidadeAte() != null && hoje.isAfter(guia.getDataValidadeAte())) {
            throw new ConflitoException("Esta Guia venceu em "
                    + guia.getDataValidadeAte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ". A sessão não pode mais ser assinada.");
        }

        // Trava todas as sessoes da guia e le a versao corrente (nao o snapshot do REPEATABLE READ).
        List<Sessao> sessoesDaGuia = sessaoRepository.buscarComLockPorGuia(idGuia);

        Sessao sessao = sessoesDaGuia.stream()
                .filter(s -> s.getIdSessao().equals(idSessao))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao nao encontrada: id " + idSessao));

        if (sessao.getStatus() != SituacaoSessao.SOLICITADA) {
            throw new ConflitoException("A sessao nao esta aguardando assinatura");
        }

        if (usuario.getAssinatura() == null) {
            throw new DadosInvalidosException("assinatura",
                    "Você ainda não tem assinatura cadastrada. Cadastre em Minhas informações para assinar.");
        }

        // Regra 25: nao assinar alem da quantidade autorizada do procedimento.
        // Conta a partir da lista travada (current read), nao de um countBy no snapshot.
        GuiaProcedimento procedimento = sessao.getGuiaProcedimento();
        long jaAssinadas = sessoesDaGuia.stream()
                .filter(s -> s.getGuiaProcedimento().getIdGuiaProcedimento()
                        .equals(procedimento.getIdGuiaProcedimento()))
                .filter(s -> s.getStatus() == SituacaoSessao.ASSINADA)
                .count();
        if (jaAssinadas >= procedimento.getQuantidadeAutorizada()) {
            throw new ConflitoException(
                    "Todas as sessoes autorizadas deste procedimento ja foram assinadas");
        }

        sessao.setStatus(SituacaoSessao.ASSINADA);
        sessao.setDataAssinatura(LocalDateTime.now());
        sessao.setAssinadoPor(usuario);
        sessao.setAssinatura(usuario.getAssinatura());

        // A lista travada contem a mesma instancia gerenciada: o novo status e visivel nela.
        boolean todasAssinadas = sessoesDaGuia.stream()
                .allMatch(s -> s.getStatus() == SituacaoSessao.ASSINADA);
        guia.setSituacaoExecucao(todasAssinadas
                ? SituacaoExecucao.REALIZADA
                : SituacaoExecucao.EM_EXECUCAO);

        List<ProcedimentoDto> procedimentosDto = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(idGuia)).stream()
                .map(ProcedimentoDto::fromEntity)
                .toList();
        List<SessaoDto> sessoesDto = sessoesDaGuia.stream()
                .map(SessaoDto::fromEntity)
                .toList();

        return GuiaBeneficiarioResponse.fromEntity(guia, procedimentosDto, sessoesDto,
                PacienteDto.de(guia.getPreGuia(), usuario));
    }
}