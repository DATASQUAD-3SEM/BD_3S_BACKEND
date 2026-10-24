package fatec.fusex.nexus.ocs;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.ocs.dto.OcsBuscaResponse;
import fatec.fusex.nexus.ocs.dto.OcsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public OcsBuscaResponse buscar(String termo) {
        Pageable pageable = PageRequest.of(0, 30, Sort.by("nome").ascending());
        Page<Ocs> pagina = (termo == null || termo.isBlank())
                ? ocsRepository.findByAtivoTrue(pageable)
                : ocsRepository.buscarPorTermo(termo.trim(), pageable);

        List<OcsResponse> resultados = pagina.getContent().stream().map(OcsResponse::de).toList();
        return new OcsBuscaResponse(resultados, pagina.getTotalElements());
    }

}