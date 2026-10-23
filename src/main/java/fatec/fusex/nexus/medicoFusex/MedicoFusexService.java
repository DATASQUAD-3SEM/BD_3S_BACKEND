package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import org.springframework.stereotype.Service;

@Service
public class MedicoFusexService {

    private final MedicoFusexRepository medicoRepository;

    public MedicoFusexService(MedicoFusexRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    /** T2-BE2: dados do medico logado. O login vem de ContextoAutenticacao, nunca do cliente. */
    public MedicoMeResponse buscarLogado(String login) {
        return medicoRepository.findByUsuarioLoginAndUsuarioAtivoTrue(login)
                .map(MedicoMeResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medico nao encontrado"));
    }
}