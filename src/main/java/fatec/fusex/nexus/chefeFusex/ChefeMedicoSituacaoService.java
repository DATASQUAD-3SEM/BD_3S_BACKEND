package fatec.fusex.nexus.chefeFusex;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.chefeFusex.dto.AlterarSituacaoMedicoRequest;
import fatec.fusex.nexus.chefeFusex.dto.MedicoSituacaoResponse;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T8-BE4: desativar e reativar medico.
 * Nunca faz DELETE: so altera Usuario.ativo, para preservar pre-guias, Guias e assinaturas.
 * O bloqueio de login e de assinatura do inativo fica nos endpoints de login e de assinatura.
 */
@Service
public class ChefeMedicoSituacaoService {

    private final MedicoFusexRepository medicoRepository;
    private final UsuarioLogado usuarioLogado;

    public ChefeMedicoSituacaoService(MedicoFusexRepository medicoRepository,
                                      UsuarioLogado usuarioLogado) {
        this.medicoRepository = medicoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Ordem: perfil (403) -> existe (404) -> corpo (422). Repetir a mesma situacao devolve 200 (idempotente).
    @Transactional
    public MedicoSituacaoResponse alterar(Long id, AlterarSituacaoMedicoRequest request) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);

        MedicoFusex medico = medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medico nao encontrado"));

        if (request == null || request.ativo() == null) {
            throw new DadosInvalidosException("ativo", "Campo obrigatorio");
        }

        Usuario usuario = medico.getUsuario();
        usuario.setAtivo(request.ativo());

        return new MedicoSituacaoResponse(medico.getUsuarioId(), usuario.getAtivo());
    }
}
