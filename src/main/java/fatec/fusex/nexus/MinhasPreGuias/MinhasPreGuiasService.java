package fatec.fusex.nexus.MinhasPreGuias;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.MinhasPreGuias.dto.MinhaPreGuiaResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MinhasPreGuiasService {

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final GuiaRepository guiaRepository;

    public MinhasPreGuiasService(PreGuiaRepository preGuiaRepository,
                                 BeneficiarioRepository beneficiarioRepository,
                                 PreGuiaHistoricoRepository historicoRepository,
                                 GuiaRepository guiaRepository) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.historicoRepository = historicoRepository;
        this.guiaRepository = guiaRepository;
    }

    @Transactional(readOnly = true)
    public List<MinhaPreGuiaResponse> listar(String loginDoToken) {
        Beneficiario beneficiario = buscarBeneficiario(loginDoToken);

        List<PreGuia> preGuias = preGuiaRepository.findByBeneficiarioUsuarioIdOrderByDataCriacaoDesc(beneficiario.getUsuarioId());

        return preGuias.stream().map(preGuia -> {

            // 1. Regra: observacao só em ATENCAO
            String obs = null;
            if (preGuia.getStatus() == PreGuiaStatus.ATENCAO) {
                obs = historicoRepository
                        .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                        .map(PreGuiaHistorico::getObservacao)
                        .orElse(null);
            }

            // 2. Regra: guia só em CONCLUIDA
            MinhaPreGuiaResponse.GuiaDto guiaDto = null;
            if (preGuia.getStatus() == PreGuiaStatus.CONCLUIDA) {
                guiaDto = guiaRepository.findByPreGuiaIdPreGuia(preGuia.getIdPreGuia())
                        .map(guia -> new MinhaPreGuiaResponse.GuiaDto(
                                guia.getNumero(),
                                // REGRA DE NEGÓCIO: Validade de 2 meses após a emissão
                                guia.getDataInsercao().toLocalDate().plusMonths(2)
                        ))
                        .orElse(null);
            }

            return MinhaPreGuiaResponse.fromEntity(preGuia, obs, guiaDto);

        }).toList();
    }

    private Beneficiario buscarBeneficiario(String login) {
        return beneficiarioRepository.findByCpf(login)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiário não encontrado."));
    }
}