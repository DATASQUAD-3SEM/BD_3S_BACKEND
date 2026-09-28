package fatec.fusex.nexus.ocs;

import java.util.List;

import org.springframework.stereotype.Service;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;

/**
 * Service = onde ficam as REGRAS. Controller so recebe/devolve; repository so acessa o banco.
 * Este e o exemplo do padrao: veja OcsServiceTest para copiar o jeito de testar com Mockito.
 */
@Service
public class OcsService {

    private final OcsRepository ocsRepository;

    public OcsService(OcsRepository ocsRepository) {
        this.ocsRepository = ocsRepository;
    }

    public List<Ocs> listar() {
        return ocsRepository.findAll();
    }

    public Ocs buscarPorId(Long id) {
        return ocsRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("OCS nao encontrada: id " + id));
    }
}
