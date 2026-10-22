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
        // Fixa em 30 resultados, ordenando por nome (A-Z)
        Pageable pageable = PageRequest.of(0, 30, Sort.by("nome").ascending());

        Page<Ocs> pagina;

        if (termo == null || termo.isBlank()) {
            // Se não tem busca, traz a lista inicial (todas)
            pagina = ocsRepository.findAll(pageable);
        } else {
            // Se tem busca, filtra pelo termo
            pagina = ocsRepository.buscarPorTermo(termo.trim(), pageable);
        }

        // Reaproveita o OcsResponse que já existe no projeto
        List<OcsResponse> resultados = pagina.getContent()
                .stream()
                .map(OcsResponse::de)
                .toList();

        // Retorna a lista + o total de registros (mesmo que o total seja maior que 30)
        return new OcsBuscaResponse(resultados, pagina.getTotalElements());
    }
}