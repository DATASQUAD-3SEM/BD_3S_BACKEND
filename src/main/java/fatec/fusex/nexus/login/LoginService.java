package fatec.fusex.nexus.login;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeFusex.ChefeFusexRepository;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.login.dto.LoginRequest;
import fatec.fusex.nexus.login.dto.LoginResponse;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Regras do login (explicacoes das telas, item LOGIN):
 * - o campo aceita CPF ou nome de usuario. Se o texto tiver formato de CPF (com ou sem pontos e traco),
 *   a busca e feita pelos 11 digitos, como o CPF fica gravado em usuario.login; senao, o texto e usado como digitado;
 * - login inexistente ou senha errada devolvem a mesma mensagem generica (401), sem dizer qual dado errou;
 * - conta desativada nao entra (403), e esse aviso so aparece depois de a senha conferir.
 */
@Service
public class LoginService {

    static final String MENSAGEM_GENERICA = "CPF, usuario ou senha incorretos.";
    private static final Pattern FORMATO_CPF = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");

    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final ChefeFusexRepository chefeRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UsuarioRepository usuarioRepository,
                        BeneficiarioRepository beneficiarioRepository,
                        MedicoFusexRepository medicoRepository,
                        ChefeFusexRepository chefeRepository,
                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.medicoRepository = medicoRepository;
        this.chefeRepository = chefeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse entrar(LoginRequest request) {
        String informado = request == null || request.login() == null ? "" : request.login().trim();
        String senha = request == null || request.senha() == null ? "" : request.senha();
        if (informado.isEmpty() || senha.isEmpty()) {
            throw new NaoAutenticadoException(MENSAGEM_GENERICA);
        }

        Usuario usuario = usuarioRepository.findByLogin(normalizarLogin(informado))
                .filter(u -> passwordEncoder.matches(senha, u.getSenhaHash()))
                .orElseThrow(() -> new NaoAutenticadoException(MENSAGEM_GENERICA));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Conta desativada. Procure o Chefe do FUSEX.");
        }

        return new LoginResponse(usuario.getLogin(), usuario.getPerfil().name(), nomeDe(usuario));
    }

    static String normalizarLogin(String informado) {
        return FORMATO_CPF.matcher(informado).matches() ? informado.replaceAll("\\D", "") : informado;
    }

    // Contas de setor nao tem pessoa atribuida: o nome exibido e o do proprio setor
    private String nomeDe(Usuario usuario) {
        Long id = usuario.getIdUsuario();
        return switch (usuario.getPerfil()) {
            case BENEFICIARIO -> beneficiarioRepository.findById(id).map(b -> b.getNome()).orElse(usuario.getLogin());
            case MEDICO -> medicoRepository.findById(id).map(m -> m.getNome()).orElse(usuario.getLogin());
            case CHEFE_FUSEX -> chefeRepository.findById(id).map(c -> c.getNome()).orElse(usuario.getLogin());
            case SETOR_GUIAS -> "Setor de Guias";
            case RECEPCAO -> "Recepcao";
        };
    }
}
