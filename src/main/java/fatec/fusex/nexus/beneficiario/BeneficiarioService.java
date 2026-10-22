package fatec.fusex.nexus.beneficiario;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeneficiarioService {

    private final BeneficiarioRepository beneficiarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public BeneficiarioService(BeneficiarioRepository beneficiarioRepository,
                               UsuarioRepository usuarioRepository,
                               SimulacaoLogin simulacaoLogin) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
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

    public Beneficiario buscarPorCpf(String cpf) {
        return beneficiarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
    }

    @Transactional(readOnly = true)
    public BeneficiarioResponse buscarLogado() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.BENEFICIARIO) {
            throw new AcessoNegadoException(
                    "Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO");
        }

        Beneficiario beneficiario = beneficiarioRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));

        return BeneficiarioResponse.de(beneficiario);
    }
}