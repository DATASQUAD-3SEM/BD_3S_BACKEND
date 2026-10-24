package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import org.springframework.stereotype.Service;

@Service
public class MedicoFusexService {

    private final MedicoFusexRepository medicoRepository;

    // Construtor recebendo APENAS o repositório necessário para a T2-BE2
    public MedicoFusexService(MedicoFusexRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    // Valida se o usuário logado é médico e está ativo.
    public MedicoFusex validarAcessoMedico(String login) {
        MedicoFusex medico = medicoRepository.findByUsuarioLogin(login)
                .orElseThrow(() -> new AcessoNegadoException("Acesso restrito a medicos"));

        if (!medico.getUsuario().getAtivo() || !PerfilUsuario.MEDICO.equals(medico.getUsuario().getPerfil())) {
            throw new AcessoNegadoException("Acesso restrito a medicos");
        }
        return medico;
    }

    //Dados do medico logado. O login vem do Header. */
    public MedicoMeResponse buscarLogado(String login) {
        MedicoFusex medico = validarAcessoMedico(login);
        return MedicoMeResponse.de(medico);
    }
}