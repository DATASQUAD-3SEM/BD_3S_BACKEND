package fatec.fusex.nexus.ocs;

import static fatec.fusex.nexus.common.util.TextoBusca.escaparCuringas;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.ocs.dto.OcsBuscaResponse;
import fatec.fusex.nexus.ocs.dto.OcsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Busca de OCS ativas por nome, contrato ou tipo (T2-BE2), limitada a 30 resultados e com o campo total.
 */
@Service
public class OcsService {

    private final OcsRepository ocsRepository;

    public OcsService(OcsRepository ocsRepository) {
        this.ocsRepository = ocsRepository;
    }

    public Ocs buscarPorId(Long id) {
        return ocsRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("OCS não encontrada com o id: " + id));
    }

    private static final int LIMITE_RESULTADOS = 30;

    public OcsBuscaResponse buscar(String termo) {
        Pageable pageable = PageRequest.of(0, LIMITE_RESULTADOS, Sort.by("nome").ascending());
        Page<Ocs> pagina = (termo == null || termo.isBlank())
                ? ocsRepository.findByAtivoTrue(pageable)
                : ocsRepository.buscarPorTermo(escaparCuringas(termo.trim()), pageable);

        List<OcsResponse> resultados = pagina.getContent().stream().map(OcsResponse::de).toList();
        return new OcsBuscaResponse(resultados, pagina.getTotalElements());
    }
}