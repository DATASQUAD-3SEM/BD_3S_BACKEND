package fatec.fusex.nexus.beneficiario;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Busca para o medico (T2-BE1). Aceita o Prec CP com ou sem mascara.
     * Inexistente e inativo dao a MESMA resposta (404) para nao revelar quem existe.
     * Nao registrar CPF/telefone em log (nem na mensagem das excecoes).
     */
    @Transactional(readOnly = true)
    public Beneficiario buscarAtivoPorPrecCp(String precCpInformado) {
        String precCp = PrecCp.normalizar(precCpInformado);

        return beneficiarioRepository.findWithUsuarioByPrecCp(precCp)
                .filter(b -> Boolean.TRUE.equals(b.getUsuario().getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
    }

    public Beneficiario buscarPorCpf(String cpf){
        return beneficiarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
    }
}
