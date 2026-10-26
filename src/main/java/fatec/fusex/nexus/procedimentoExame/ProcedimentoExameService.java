package fatec.fusex.nexus.procedimentoExame;

import static fatec.fusex.nexus.common.util.TextoBusca.escaparCuringas;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Busca no catalogo de procedimentos para a Nova pre-guia do medico (T2-BE3):
 * minimo de 2 caracteres (400), no maximo 30 resultados e ordem alfabetica.
 */
@Service
public class ProcedimentoExameService {

    private static final int LIMITE_RESULTADOS = 30;
    private static final int MINIMO_CARACTERES = 2;

    private final ProcedimentoExameRepository procedimentoRepository;

    public ProcedimentoExameService(ProcedimentoExameRepository procedimentoRepository) {
        this.procedimentoRepository = procedimentoRepository;
    }

    // T2-BE3: busca por nome ou codigo TUSS no catalogo inteiro, sem depender da OCS.
    public List<ProcedimentoMedicoResponse> buscar(String busca) {
        if (busca == null || busca.trim().length() < MINIMO_CARACTERES) {
            throw new RegraDeNegocioException("A busca deve ter no minimo " + MINIMO_CARACTERES + " caracteres");
        }

        Pageable pageable = PageRequest.of(0, LIMITE_RESULTADOS, Sort.by("terminologiaProcedimento").ascending());
        return procedimentoRepository.buscarPorTermo(escaparCuringas(busca.trim()), pageable)
                .stream()
                .map(ProcedimentoMedicoResponse::de)
                .toList();
    }
}
