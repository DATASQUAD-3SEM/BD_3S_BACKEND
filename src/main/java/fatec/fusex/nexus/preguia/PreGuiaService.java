package fatec.fusex.nexus.preguia;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsService;
import fatec.fusex.nexus.preguia.dto.PreGuiaResponse;
import fatec.fusex.nexus.procedimento.ProcedimentoExame;
import fatec.fusex.nexus.procedimento.ProcedimentoExameRepository;

/**
 * SCRUM-22 - Validacao do vinculo PreGuia <-> Beneficiario.
 * SCRUM-30/31 - Criacao da pre-guia (POST /pre-guias multipart) e validacao dos
 * procedimentos informados. Ver PreGuiaController para o endpoint.
 *
 * Nesta Sprint nao existe login: o "beneficiario logado" e passado por parametro.
 * Sprint 2 (US7/US8): trocar o parametro por @AuthenticationPrincipal / JWT.
 *
 * O vinculo em si (coluna beneficiario_id) ja existe na entidade PreGuia.
 * O que este service garante e que ninguem acessa uma PreGuia que nao e sua.
 */
@Service
public class PreGuiaService {

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final ProcedimentoExameRepository procedimentoExameRepository;
    private final OcsService ocsService;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    public PreGuiaService(PreGuiaRepository preGuiaRepository,
                           BeneficiarioRepository beneficiarioRepository,
                           ProcedimentoExameRepository procedimentoExameRepository,
                           OcsService ocsService,
                           ArmazenamentoArquivo armazenamentoArquivo) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.procedimentoExameRepository = procedimentoExameRepository;
        this.ocsService = ocsService;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    /**
     * Lista todas as pre-guias de um beneficiario.
     * Valida que o beneficiario existe antes de consultar (senao devolveria lista vazia por engano).
     */
    public List<PreGuia> listarDoBeneficiario(Long beneficiarioId) {
        if (!beneficiarioRepository.existsById(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Beneficiario nao encontrado: id " + beneficiarioId);
        }
        return preGuiaRepository.findByBeneficiarioId(beneficiarioId);
    }

    /**
     * Busca uma pre-guia por id E confirma que ela pertence ao beneficiario informado.
     * Se a pre-guia existir mas for de outro beneficiario, devolvemos 404 (mesma resposta
     * de "nao encontrada") — assim nao revelamos para o atacante que a pre-guia existe.
     */
    public PreGuia buscarDoBeneficiario(Long preGuiaId, Long beneficiarioId) {
        PreGuia preGuia = preGuiaRepository.findById(preGuiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId));

        if (!preGuia.getBeneficiario().getId().equals(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId);
        }

        return preGuia;
    }

    /**
     * SCRUM-30: cria a pre-guia. Absorve o antigo /encaminhamentos (SCRUM-18, ja removido):
     * o arquivo chega junto com o resto dos dados na mesma requisicao multipart.
     *
     * A pre-guia nasce RASCUNHO (default da entidade, SCRUM-32) e e imediatamente
     * confirmada via confirmarEnvio() (RASCUNHO -> PENDENTE) dentro deste mesmo metodo:
     * nao existe um passo de rascunho exposto na API, tudo acontece numa unica requisicao.
     * Se algo estiver errado, o beneficiario troca o arquivo no formulario (componente
     * UploadEncaminhamento, SCRUM-23) e reenvia tudo de novo.
     *
     * Ordem de validacao: arquivo -> OCS -> procedimentos (SCRUM-31) -> beneficiario -> salvar.
     *
     * Devolve o DTO (nao a entidade) montado AQUI DENTRO, ainda na transacao: ocs e
     * procedimentos sao LAZY, e o Controller roda fora da transacao do Service.
     */
    @Transactional
    public PreGuiaResponse criar(String cpf, String precCp, Long ocsId, List<Long> procedimentoIds, MultipartFile arquivo) {
        validarArquivoObrigatorio(arquivo);

        Ocs ocs = ocsService.buscarPorId(ocsId);
        Set<ProcedimentoExame> procedimentos = validarProcedimentosExistem(procedimentoIds);
        validarProcedimentosDaOcs(procedimentos, ocs);
        Beneficiario beneficiario = identificarBeneficiario(cpf, precCp);

        String encaminhamentoUrl = armazenamentoArquivo.salvar(arquivo, "encaminhamentos");
        try {
            PreGuia preGuia = new PreGuia();
            preGuia.setBeneficiario(beneficiario);
            preGuia.setOcs(ocs);
            preGuia.setEncaminhamentoUrl(encaminhamentoUrl);
            preGuia.getProcedimentos().addAll(procedimentos);
            preGuia.confirmarEnvio();

            preGuiaRepository.save(preGuia);

            return PreGuiaResponse.de(preGuia);
        } catch (RuntimeException e) {
            // o banco volta sozinho (rollback); o arquivo precisa sair na mao
            try {
                armazenamentoArquivo.remover(encaminhamentoUrl);
            } catch (RuntimeException limpezaFalhou) {
                // nao deixa a falha da limpeza esconder a causa original
                // (Sprint 1 nao tem logger configurado; em Sprint 2 trocar por log.warn)
            }
            throw e;
        }
    }

    /**
     * SCRUM-22 (reaproveitado aqui): identifica o beneficiario sem login (Sprint 1 ainda
     * nao tem JWT - ver docs/DECISOES_PENDENTES.md). O front manda cpf + precCp
     * (CONTRATO_API.md); exigimos os dois baterem com o MESMO cadastro.
     *
     * ATENCAO: isto so funciona se cpf/precCp chegarem SEM formatacao (so digitos, iguais
     * ao que esta salvo no banco - VARCHAR(11) e VARCHAR(20)). O componente que capta
     * esses campos no front (DadosBeneficiario, SCRUM-33) precisa remover pontos/traco
     * antes de mandar pro back, senao o findByCpf nunca acha.
     */
    private Beneficiario identificarBeneficiario(String cpf, String precCp) {
        Beneficiario beneficiario = beneficiarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado: cpf " + cpf));

        if (!beneficiario.getPrecCp().equals(precCp)) {
            throw new RegraDeNegocioException("CPF e Prec-CP nao correspondem ao mesmo beneficiario");
        }
        return beneficiario;
    }

    /**
     * SCRUM-28: nao existe pre-guia sem encaminhamento medico anexado. Sem estado de
     * rascunho (decisao confirmada com o time) - o arquivo e obrigatorio ja na criacao,
     * nao so numa etapa posterior.
     */
    private void validarArquivoObrigatorio(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("E necessario anexar o encaminhamento medico para criar a pre-guia");
        }
    }

    /**
     * SCRUM-31: cada procedimento informado precisa existir no catalogo procedimento_exame.
     */
    private Set<ProcedimentoExame> validarProcedimentosExistem(List<Long> procedimentoIds) {
        if (procedimentoIds == null || procedimentoIds.isEmpty()) {
            throw new RegraDeNegocioException("E necessario informar ao menos um procedimento/exame");
        }

        Set<Long> idsUnicos = new LinkedHashSet<>(procedimentoIds);
        List<ProcedimentoExame> encontrados = procedimentoExameRepository.findAllById(idsUnicos);

        if (encontrados.size() < idsUnicos.size()) {
            Set<Long> idsEncontrados = encontrados.stream().map(ProcedimentoExame::getId).collect(Collectors.toSet());
            Set<Long> faltando = new LinkedHashSet<>(idsUnicos);
            faltando.removeAll(idsEncontrados);
            throw new RecursoNaoEncontradoException("Procedimento(s) nao encontrado(s): " + faltando);
        }

        return new LinkedHashSet<>(encontrados);
    }

    /**
     * SCRUM-31 (complemento): nao basta existir no catalogo - o procedimento precisa
     * estar na relacao ocs_procedimento da OCS escolhida (mapeada em Ocs.procedimentos).
     * Sem isso nao ha garantia de que a OCS escolhida realmente realiza o exame pedido.
     */
    private void validarProcedimentosDaOcs(Set<ProcedimentoExame> procedimentos, Ocs ocs) {
        Set<Long> idsDaOcs = ocs.getProcedimentos().stream().map(ProcedimentoExame::getId).collect(Collectors.toSet());
        List<String> foraDaOcs = procedimentos.stream()
                .filter(p -> !idsDaOcs.contains(p.getId()))
                .map(ProcedimentoExame::getTerminologiaProcedimentoEvento)
                .toList();

        if (!foraDaOcs.isEmpty()) {
            throw new RegraDeNegocioException("A OCS escolhida nao realiza: " + String.join(", ", foraDaOcs));
        }
    }
}
