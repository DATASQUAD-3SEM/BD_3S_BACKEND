package fatec.fusex.nexus.chefeBeneficiario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeBeneficiario.dto.AlterarSituacaoBeneficiarioRequest;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioSituacaoResponse;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T7-BE4: desativar e reativar beneficiario.
 * Nunca faz DELETE: so altera Usuario.ativo, para preservar pre-guias, Guias e assinaturas.
 * O bloqueio de login do inativo fica no endpoint de login.
 */
@Service
public class ChefeBeneficiarioSituacaoService {

    private final BeneficiarioRepository beneficiarioRepository;
    private final UsuarioLogado usuarioLogado;

    public ChefeBeneficiarioSituacaoService(BeneficiarioRepository beneficiarioRepository,
                                            UsuarioLogado usuarioLogado) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Ordem: perfil (403) -> existe (404) -> corpo (422). Repetir a mesma situacao devolve 200 (idempotente).
    @Transactional
    public BeneficiarioSituacaoResponse alterar(Long id, AlterarSituacaoBeneficiarioRequest request) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);

        Beneficiario beneficiario = beneficiarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));

        if (request == null || request.ativo() == null) {
            throw new DadosInvalidosException("ativo", "Campo obrigatorio");
        }

        Usuario usuario = beneficiario.getUsuario();
        usuario.setAtivo(request.ativo());

        return new BeneficiarioSituacaoResponse(beneficiario.getUsuarioId(), usuario.getAtivo());
    }
}
