package fatec.fusex.nexus.correcaoMedico;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.correcaoMedico.dto.ProcedimentoCorrecaoResponse;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;

/** T14-BE4: GET /medicos/me/procedimentos?busca= (cartao "Exames e procedimentos" da tela "Corrigir pre-guia"). */
@Service
public class ProcedimentoCorrecaoService {

    private static final int LIMITE_RESULTADOS = 30;
    private static final int MINIMO_CARACTERES = 2;

    private final ProcedimentoCorrecaoRepository procedimentoRepository;
    private final MedicoFusexService medicoService;

    public ProcedimentoCorrecaoService(ProcedimentoCorrecaoRepository procedimentoRepository,
                                       MedicoFusexService medicoService) {
        this.procedimentoRepository = procedimentoRepository;
        this.medicoService = medicoService;
    }

    // Perfil (403) vem antes da validacao do termo (400)
    @Transactional(readOnly = true)
    public List<ProcedimentoCorrecaoResponse> buscar(String login, String busca) {
        medicoService.validarPerfilMedico(login);

        if (busca == null || busca.trim().length() < MINIMO_CARACTERES) {
            throw new RegraDeNegocioException("A busca deve ter no minimo " + MINIMO_CARACTERES + " caracteres");
        }

        Pageable pageable = PageRequest.of(0, LIMITE_RESULTADOS, Sort.by("terminologiaProcedimento").ascending());
        return procedimentoRepository.buscarNoCatalogo(escaparCuringas(busca.trim()), pageable).stream()
                .map(ProcedimentoCorrecaoResponse::de)
                .toList();
    }

    // '%' e '_' digitados valem como texto (ESCAPE '!' na query).
    private static String escaparCuringas(String termo) {
        return termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
