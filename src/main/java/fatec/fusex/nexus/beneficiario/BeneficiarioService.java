package fatec.fusex.nexus.beneficiario;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeneficiarioService {

    private final BeneficiarioRepository beneficiarioRepository;

    public BeneficiarioService(BeneficiarioRepository beneficiarioRepository) {
        this.beneficiarioRepository = beneficiarioRepository;
    }

    public List<Beneficiario> listar() {
        return beneficiarioRepository.findAll();
    }

    public Beneficiario buscarPorId(Long id) {
        return beneficiarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Id nao encontrada: id " + id));
    }

    public Beneficiario buscarPorCpf(String cpf){
        return beneficiarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
    }
}
