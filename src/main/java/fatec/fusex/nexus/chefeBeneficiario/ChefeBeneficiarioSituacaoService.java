package fatec.fusex.nexus.chefeBeneficiario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeBeneficiario.dto.AlterarSituacaoBeneficiarioRequest;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioSituacaoResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T7-BE4: desativar e reativar beneficiario.
 * Nunca faz DELETE: so altera Usuario.ativo, para preservar pre-guias, Guias e assinaturas.
 * O bloqueio de login do inativo fica no endpoint de login.
 */
@Service
public class ChefeBeneficiarioSituacaoService {

    private final BeneficiarioRepository beneficiarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public ChefeBeneficiarioSituacaoService(BeneficiarioRepository beneficiarioRepository,
                                            UsuarioRepository usuarioRepository,
                                            SimulacaoLogin simulacaoLogin) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    // Ordem: perfil (403) -> existe (404) -> corpo (422). Repetir a mesma situacao devolve 200 (idempotente).
    @Transactional
    public BeneficiarioSituacaoResponse alterar(Long id, AlterarSituacaoBeneficiarioRequest request) {
        validarChefe();

        Beneficiario beneficiario = beneficiarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));

        if (request == null || request.ativo() == null) {
            throw new DadosInvalidosException("ativo", "Campo obrigatorio");
        }

        Usuario usuario = beneficiario.getUsuario();
        usuario.setAtivo(request.ativo());

        return new BeneficiarioSituacaoResponse(beneficiario.getUsuarioId(), usuario.getAtivo());
    }

    private void validarChefe() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.CHEFE_FUSEX || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Acesso restrito ao Chefe do FUSEX");
        }
    }
}
