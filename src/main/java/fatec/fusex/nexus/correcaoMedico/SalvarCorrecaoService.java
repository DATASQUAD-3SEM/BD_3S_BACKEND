package fatec.fusex.nexus.correcaoMedico;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.correcaoMedico.dto.CorrecaoReenviadaResponse;
import fatec.fusex.nexus.correcaoMedico.dto.SalvarCorrecaoRequest;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimento;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoId;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;

/**
 * T14-BE5: PUT /medicos/me/pre-guias/{id}/correcao.
 * A mesma pre-guia volta para PENDENTE: a lista de exames e substituida e a OCS muda se informada.
 */
@Service
public class SalvarCorrecaoService {

    private static final int OBSERVACAO_MAXIMO = 500; // tamanho da coluna PreGuiaHistorico.observacao

    private final MedicoFusexService medicoService;
    private final PreGuiaCorrecaoRepository preGuiaRepository;
    private final OcsRepository ocsRepository;
    private final ProcedimentoExameRepository procedimentoRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    public SalvarCorrecaoService(MedicoFusexService medicoService,
                                 PreGuiaCorrecaoRepository preGuiaRepository,
                                 OcsRepository ocsRepository,
                                 ProcedimentoExameRepository procedimentoRepository,
                                 PreGuiaProcedimentoRepository preGuiaProcedimentoRepository,
                                 PreGuiaHistoricoRepository historicoRepository) {
        this.medicoService = medicoService;
        this.preGuiaRepository = preGuiaRepository;
        this.ocsRepository = ocsRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.preGuiaProcedimentoRepository = preGuiaProcedimentoRepository;
        this.historicoRepository = historicoRepository;
    }

    // Ordem: perfil (403) -> inexistente (404) -> criada por beneficiario (403) -> de outro medico (404)
    //        -> status (409) -> corpo (422)
    @Transactional
    public CorrecaoReenviadaResponse salvar(Long id, String login, SalvarCorrecaoRequest request) {
        MedicoFusex medico = medicoService.validarAcessoMedico(login);

        // Trava a linha ate o fim da transacao (dois envios simultaneos geram uma unica mudanca)
        PreGuia preGuia = preGuiaRepository.buscarParaCorrigir(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + id));

        // Regra 11 do modelo: com medico_id nulo, quem corrige e o beneficiario; o medico nao intervem
        if (preGuia.getMedicoFusex() == null) {
            throw new AcessoNegadoException("Pre-guia criada pelo beneficiario nao pode ser corrigida pelo medico");
        }
        // Pre-guia de outro medico devolve 404, para nao revelar que ela existe
        if (!preGuia.getMedicoFusex().getUsuarioId().equals(medico.getUsuarioId())) {
            throw new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + id);
        }
        if (preGuia.getStatus() != PreGuiaStatus.ATENCAO) {
            throw new ConflitoException("A pre-guia nao esta em Atencao: ela ja foi alterada");
        }

        List<String> codigosNovos = validarProcedimentos(request == null ? null : request.procedimentos());
        Map<String, ProcedimentoExame> catalogo = buscarNoCatalogo(codigosNovos);
        Ocs ocsNova = request.ocsId() == null ? null : buscarOcsAtiva(request.ocsId());

        boolean examesMudaram = substituirProcedimentos(preGuia, codigosNovos, catalogo);
        boolean ocsMudou = ocsNova != null && !ocsNova.getIdOcs().equals(preGuia.getOcs().getIdOcs());
        if (ocsNova != null) {
            preGuia.setOcs(ocsNova);
        }

        preGuia.setStatus(PreGuiaStatus.PENDENTE);
        LocalDateTime dataReenvio = LocalDateTime.now();

        // Historico: a acao, a observacao do setor que motivou a correcao, o medico que corrigiu e a data/hora
        historicoRepository.save(new PreGuiaHistorico(
                null, preGuia, PreGuiaStatus.PENDENTE,
                textoDoHistorico(preGuia, examesMudaram, ocsMudou),
                null, medico.getUsuario(), dataReenvio));

        return new CorrecaoReenviadaResponse(preGuia.getStatus().name(), dataReenvio);
    }

    // Ao menos 1, sem repeticao (mesma regra da criacao pelo medico, T2-BE4) e sem codigo em branco
    private static List<String> validarProcedimentos(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            throw new DadosInvalidosException("procedimentos", "Mantenha ao menos um procedimento");
        }
        Set<String> unicos = new LinkedHashSet<>();
        for (String codigo : codigos) {
            String normalizado = codigo == null ? "" : codigo.trim();
            if (normalizado.isEmpty()) {
                throw new DadosInvalidosException("procedimentos", "Codigo TUSS em branco");
            }
            if (!unicos.add(normalizado)) {
                throw new DadosInvalidosException("procedimentos", "Procedimento repetido: " + normalizado);
            }
        }
        return List.copyOf(unicos);
    }

    // Catalogo inteiro: o procedimento nao precisa estar ligado a OCS
    private Map<String, ProcedimentoExame> buscarNoCatalogo(List<String> codigos) {
        Map<String, ProcedimentoExame> porCodigo = procedimentoRepository.findByCodigoTussIn(codigos).stream()
                .collect(Collectors.toMap(ProcedimentoExame::getCodigoTuss, Function.identity()));

        List<String> inexistentes = codigos.stream().filter(c -> !porCodigo.containsKey(c)).toList();
        if (!inexistentes.isEmpty()) {
            throw new DadosInvalidosException("procedimentos",
                    "Codigo TUSS inexistente no catalogo: " + String.join(", ", inexistentes));
        }
        return porCodigo;
    }

    private Ocs buscarOcsAtiva(Long ocsId) {
        return ocsRepository.findById(ocsId)
                .filter(o -> Boolean.TRUE.equals(o.getAtivo()))
                .orElseThrow(() -> new DadosInvalidosException("ocsId", "OCS inexistente ou inativa"));
    }

    // Substitui a lista pela enviada: remove o que saiu e grava so o que entrou. Devolve se algo mudou.
    private boolean substituirProcedimentos(PreGuia preGuia, List<String> codigosNovos,
                                            Map<String, ProcedimentoExame> catalogo) {
        List<PreGuiaProcedimento> atuais = preGuiaProcedimentoRepository.findByPreGuiaIdPreGuia(preGuia.getIdPreGuia());
        Set<String> codigosAtuais = atuais.stream()
                .map(pp -> pp.getProcedimentoExame().getCodigoTuss())
                .collect(Collectors.toSet());

        List<PreGuiaProcedimento> sairam = atuais.stream()
                .filter(pp -> !codigosNovos.contains(pp.getProcedimentoExame().getCodigoTuss()))
                .toList();
        preGuiaProcedimentoRepository.deleteAll(sairam);

        List<String> entraram = codigosNovos.stream().filter(c -> !codigosAtuais.contains(c)).toList();
        for (String codigo : entraram) {
            ProcedimentoExame procedimento = catalogo.get(codigo);
            preGuiaProcedimentoRepository.save(new PreGuiaProcedimento(
                    new PreGuiaProcedimentoId(preGuia.getIdPreGuia(), procedimento.getIdProcedimentoExame()),
                    preGuia,
                    procedimento));
        }
        return !sairam.isEmpty() || !entraram.isEmpty();
    }

    private String textoDoHistorico(PreGuia preGuia, boolean examesMudaram, boolean ocsMudou) {
        String acao;
        if (examesMudaram && ocsMudou) {
            acao = "exames e OCS alterados";
        } else if (examesMudaram) {
            acao = "exames alterados";
        } else if (ocsMudou) {
            acao = "OCS alterada";
        } else {
            acao = "sem alteracoes";
        }

        String observacaoDoSetor = historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                .map(PreGuiaHistorico::getObservacao)
                .orElse("-");

        String texto = "Corrigida pelo medico (" + acao + "). Observacao do setor: " + observacaoDoSetor;
        return texto.length() > OBSERVACAO_MAXIMO ? texto.substring(0, OBSERVACAO_MAXIMO) : texto;
    }
}
