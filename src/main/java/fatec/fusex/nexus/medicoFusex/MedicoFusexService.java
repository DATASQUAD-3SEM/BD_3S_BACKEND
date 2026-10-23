package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoFusexService {

    private final ProcedimentoExameRepository repository;

    public MedicoFusexService(ProcedimentoExameRepository repository) {
        this.repository = repository;
    }

    public List<ProcedimentoMedicoResponse> buscar(String busca) {
        if (busca == null || busca.trim().length() < 2) {
            throw new RegraDeNegocioException("A busca deve conter no mínimo 2 caracteres.");
        }

        String termo = busca.trim();

        return repository.findTop30ByTerminologiaProcedimentoContainingIgnoreCaseOrCodigoTussContainingIgnoreCaseOrderByTerminologiaProcedimentoAsc(termo, termo)
                .stream()
                .map(ProcedimentoMedicoResponse::de)
                .toList();
    }
}