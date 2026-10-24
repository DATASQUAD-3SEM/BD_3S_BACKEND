package fatec.fusex.nexus.preGuia;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.dto.CriarPreGuiaMedicoRequest;
import fatec.fusex.nexus.preGuia.dto.PreGuiaMedicoResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimento;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoId;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;

/**
 * T2-BE4: o medico cria e envia a pre-guia durante a consulta, sem encaminhamento.
 * Fica separado do PreGuiaService (rota do beneficiario) para nao misturar as duas regras.
 */
@Service
public class MedicoPreGuiaService {

    private final MedicoFusexService medicoService;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final ProcedimentoExameRepository procedimentoRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    public MedicoPreGuiaService(MedicoFusexService medicoService,
                                BeneficiarioRepository beneficiarioRepository,
                                OcsRepository ocsRepository,
                                ProcedimentoExameRepository procedimentoRepository,
                                PreGuiaRepository preGuiaRepository,
                                PreGuiaProcedimentoRepository preGuiaProcedimentoRepository,
                                PreGuiaHistoricoRepository historicoRepository) {
        this.medicoService = medicoService;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.preGuiaProcedimentoRepository = preGuiaProcedimentoRepository;
        this.historicoRepository = historicoRepository;
    }

    // Ordem: perfil (403) -> beneficiario -> OCS -> procedimentos (422 apontando o campo)
    @Transactional
    public PreGuiaMedicoResponse criar(String login, CriarPreGuiaMedicoRequest request) {
        MedicoFusex medico = medicoService.validarAcessoMedico(login);

        Beneficiario beneficiario = buscarBeneficiarioAtivo(request.beneficiarioId());
        Ocs ocs = buscarOcsAtiva(request.ocsId());
        List<ProcedimentoExame> procedimentos = buscarProcedimentos(request.procedimentos());

        // Sem encaminhamento: o banco aceita porque medico_id esta preenchido (ck_preguia_encaminhamento_url)
        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setMedicoFusex(medico);
        preGuia.setStatus(PreGuiaStatus.PENDENTE);
        preGuiaRepository.save(preGuia);

        for (ProcedimentoExame procedimento : procedimentos) {
            preGuiaProcedimentoRepository.save(new PreGuiaProcedimento(
                    new PreGuiaProcedimentoId(preGuia.getIdPreGuia(), procedimento.getIdProcedimentoExame()),
                    preGuia,
                    procedimento));
        }

        // Primeira linha do historico: quem criou foi o medico
        historicoRepository.save(new PreGuiaHistorico(
                null, preGuia, PreGuiaStatus.PENDENTE, null, null, medico.getUsuario(), preGuia.getDataCriacao()));

        // O numero sai do id (IDENTITY): dois envios ao mesmo tempo nunca recebem o mesmo
        return PreGuiaMedicoResponse.de(preGuia);
    }

    private Beneficiario buscarBeneficiarioAtivo(Long beneficiarioId) {
        if (beneficiarioId == null) {
            throw new DadosInvalidosException("beneficiarioId", "Informe o beneficiario");
        }
        return beneficiarioRepository.findById(beneficiarioId)
                .filter(b -> Boolean.TRUE.equals(b.getUsuario().getAtivo()))
                .orElseThrow(() -> new DadosInvalidosException("beneficiarioId", "Beneficiario inexistente ou inativo"));
    }

    private Ocs buscarOcsAtiva(Long ocsId) {
        if (ocsId == null) {
            throw new DadosInvalidosException("ocsId", "Informe a OCS");
        }
        return ocsRepository.findById(ocsId)
                .filter(o -> Boolean.TRUE.equals(o.getAtivo()))
                .orElseThrow(() -> new DadosInvalidosException("ocsId", "OCS inexistente ou inativa"));
    }

    // Busca no catalogo inteiro: o procedimento nao precisa estar ligado a OCS escolhida
    private List<ProcedimentoExame> buscarProcedimentos(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            throw new DadosInvalidosException("procedimentos", "Inclua ao menos um procedimento");
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

        Map<String, ProcedimentoExame> porCodigo = procedimentoRepository.findByCodigoTussIn(unicos).stream()
                .collect(Collectors.toMap(ProcedimentoExame::getCodigoTuss, Function.identity()));

        List<String> inexistentes = unicos.stream().filter(c -> !porCodigo.containsKey(c)).toList();
        if (!inexistentes.isEmpty()) {
            throw new DadosInvalidosException("procedimentos",
                    "Codigo TUSS inexistente no catalogo: " + String.join(", ", inexistentes));
        }

        return unicos.stream().map(porCodigo::get).toList();
    }
}
