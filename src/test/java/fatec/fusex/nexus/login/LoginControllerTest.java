package fatec.fusex.nexus.login;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Testes de POST /auth/login com banco em memoria: CPF com e sem mascara, nome de usuario,
 * mensagem generica para login ou senha errados e bloqueio de conta desativada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class LoginControllerTest {

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final PasswordEncoder passwordEncoder;

    LoginControllerTest(MockMvc mockMvc, UsuarioRepository usuarioRepository,
                        BeneficiarioRepository beneficiarioRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void preparar() {
        Usuario beneficiario = usuario("80000000201", PerfilUsuario.BENEFICIARIO, true);
        Beneficiario dados = new Beneficiario();
        dados.setUsuario(beneficiario);
        dados.setNome("Joana Teste");
        dados.setPrecCp("80000000201");
        dados.setDataNascimento(LocalDate.of(1980, 1, 1));
        dados.setTelefone("12987654321");
        beneficiarioRepository.save(dados);

        usuario("setor.login.teste", PerfilUsuario.SETOR_GUIAS, true);
        usuario("80000000302", PerfilUsuario.BENEFICIARIO, false);
    }

    private Usuario usuario(String login, PerfilUsuario perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash(passwordEncoder.encode("senha123"));
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuarioRepository.save(usuario);
    }

    private ResultActions entrar(String login, String senha) throws Exception {
        String corpo = "{\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}";
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void cpfComMascara_entraComoBeneficiario() throws Exception {
        entrar("800.000.002-01", "senha123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("80000000201"))
                .andExpect(jsonPath("$.perfil").value("BENEFICIARIO"))
                .andExpect(jsonPath("$.nome").value("Joana Teste"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void cpfSemMascara_entra() throws Exception {
        entrar("80000000201", "senha123").andExpect(status().isOk());
    }

    @Test
    void nomeDeUsuario_entraComoSetor() throws Exception {
        entrar("setor.login.teste", "senha123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("SETOR_GUIAS"))
                .andExpect(jsonPath("$.nome").value("Setor de Guias"));
    }

    @Test
    void senhaErrada_devolve401ComMensagemGenerica() throws Exception {
        entrar("80000000201", "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(LoginService.MENSAGEM_GENERICA));
    }

    @Test
    void loginInexistente_devolveAMesmaMensagem() throws Exception {
        entrar("nao.existe", "senha123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(LoginService.MENSAGEM_GENERICA));
    }

    @Test
    void camposVazios_devolve401() throws Exception {
        entrar("", "").andExpect(status().isUnauthorized());
    }

    @Test
    void contaDesativada_naoEntra() throws Exception {
        entrar("80000000302", "senha123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Conta desativada. Procure o Chefe do FUSEX."));
    }

    @Test
    void contaDesativadaComSenhaErrada_naoRevelaQueAContaExiste() throws Exception {
        entrar("80000000302", "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(LoginService.MENSAGEM_GENERICA));
    }
}
