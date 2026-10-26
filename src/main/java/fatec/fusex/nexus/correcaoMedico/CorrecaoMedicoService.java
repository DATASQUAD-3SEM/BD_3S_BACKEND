package fatec.fusex.nexus.correcaoMedico;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.correcaoMedico.dto.CorrecaoMedicoResponse;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;

/** T14-BE2: GET /medicos/me/pre-guias/{id}/correcao. */
@Service
public class CorrecaoMedicoService {

    private final MedicoFusexService medicoService;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    public CorrecaoMedicoService(MedicoFusexService medicoService,
                                 PreGuiaRepository preGuiaRepository,
                                 PreGuiaProcedimentoRepository preGuiaProcedimentoRepository,
                                 PreGuiaHistoricoRepository historicoRepository) {
        this.medicoService = medicoService;
        this.preGuiaRepository = preGuiaRepository;
        this.preGuiaProcedimentoRepository = preGuiaProcedimentoRepository;
        this.historicoRepository = historicoRepository;
    }

    // Ordem: perfil (403) -> inexistente (404) -> criada por beneficiario (403) -> de outro medico (404) -> status (409)
    @Transactional(readOnly = true)
    public CorrecaoMedicoResponse buscar(Long id, String login) {
        MedicoFusex medico = medicoService.validarAcessoMedico(login);

        PreGuia preGuia = preGuiaRepository.findById(id)
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
            throw new ConflitoException("A pre-guia nao esta em Atencao");
        }

        return new CorrecaoMedicoResponse(
                String.format("%04d", preGuia.getIdPreGuia()),
                preGuia.getStatus().name(),
                beneficiario(preGuia.getBeneficiario()),
                ocs(preGuia.getOcs()),
                procedimentos(preGuia),
                observacao(preGuia));
    }

    private static CorrecaoMedicoResponse.BeneficiarioResumo beneficiario(Beneficiario b) {
        int idade = b.idadeEm(Beneficiario.hoje());
        return new CorrecaoMedicoResponse.BeneficiarioResumo(
                b.getNome(), b.getPrecCp(), FaixaEtaria.de(idade).getRotulo(), idade, b.getTelefone());
    }

    private static CorrecaoMedicoResponse.OcsResumo ocs(Ocs ocs) {
        return new CorrecaoMedicoResponse.OcsResumo(
                ocs.getIdOcs(), ocs.getNome(), ocs.getContratoNum(), ocs.getTipo().name());
    }

    private List<CorrecaoMedicoResponse.ProcedimentoResumo> procedimentos(PreGuia preGuia) {
        return preGuiaProcedimentoRepository.findByPreGuiaIdPreGuia(preGuia.getIdPreGuia()).stream()
                .map(pp -> new CorrecaoMedicoResponse.ProcedimentoResumo(
                        pp.getProcedimentoExame().getCodigoTuss(),
                        pp.getProcedimentoExame().getTerminologiaProcedimento()))
                .sorted(Comparator.comparing(CorrecaoMedicoResponse.ProcedimentoResumo::nome))
                .toList();
    }

    // A observacao do setor vive no historico: e a linha ATENCAO mais recente
    private CorrecaoMedicoResponse.ObservacaoResumo observacao(PreGuia preGuia) {
        return historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                .map(h -> new CorrecaoMedicoResponse.ObservacaoResumo(h.getObservacao(), h.getDataHora()))
                .orElse(null);
    }
}
