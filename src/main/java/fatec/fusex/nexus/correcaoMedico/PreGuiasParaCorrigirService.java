package fatec.fusex.nexus.correcaoMedico;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.correcaoMedico.dto.PreGuiaParaCorrigirResponse;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;

/**
 * T14-BE1: GET /medicos/me/pre-guias/para-corrigir.
 * So as pre-guias criadas pelo medico logado (medico_id) que o setor de Guias devolveu (ATENCAO).
 * Pre-guia criada por beneficiario tem medico_id nulo, entao nunca entra aqui.
 */
@Service
public class PreGuiasParaCorrigirService {

    private final MedicoFusexService medicoService;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    public PreGuiasParaCorrigirService(MedicoFusexService medicoService,
                                       PreGuiaRepository preGuiaRepository,
                                       PreGuiaHistoricoRepository historicoRepository) {
        this.medicoService = medicoService;
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public List<PreGuiaParaCorrigirResponse> listar(String login) {
        MedicoFusex medico = medicoService.validarAcessoMedico(login); // 403 se nao for medico ativo

        return preGuiaRepository
                .findByMedicoFusexUsuarioIdAndStatusOrderByDataCriacaoAscIdPreGuiaAsc(
                        medico.getUsuarioId(), PreGuiaStatus.ATENCAO)
                .stream()
                .map(p -> PreGuiaParaCorrigirResponse.de(p, observacaoMaisRecente(p)))
                .toList();
    }

    // A observacao do setor vive no historico: e a linha ATENCAO mais recente
    private String observacaoMaisRecente(PreGuia preGuia) {
        return historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                .map(PreGuiaHistorico::getObservacao)
                .orElse(null);
    }
}
