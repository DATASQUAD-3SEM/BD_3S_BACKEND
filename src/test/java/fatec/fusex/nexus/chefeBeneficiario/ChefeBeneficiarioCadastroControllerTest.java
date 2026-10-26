package fatec.fusex.nexus.chefeBeneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
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
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** Ponta a ponta (filtro de login + controller + service + H2) do POST /chefe/beneficiarios. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeBeneficiarioCadastroControllerTest {

    private static final String URL = "/chefe/beneficiarios";
    private static final String CHEFE = "chefe.teste";

    // CPFs validos (digitos verificadores conferem)
    private static final String CPF_NOVO = "39053344705";
    private static final String CPF_BENEFICIARIO_EXISTENTE = "52998224725";
    private static final String CPF_MEDICO_EXISTENTE = "11144477735";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;

    ChefeBeneficiarioCadastroControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            MedicoFusexRepository medicoRepository,
            PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.medicoRepository = medicoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void dados() {
        usuario(CHEFE, PerfilUsuario.CHEFE_FUSEX);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(CPF_BENEFICIARIO_EXISTENTE, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);

        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario(CPF_MEDICO_EXISTENTE, PerfilUsuario.MEDICO));
        medico.setNome("Carlos Eduardo Lima");
        medico.setPatente("Capitao");
        medico.setPrecCp("11122233344");
        medico.setCrm("123456-SP");
        medicoRepository.saveAndFlush(medico);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private static String corpo(String cpf, String precCp, String dataNascimento, String telefone, String senha) {
        return """
                {"nome":"Marta Souza Lima","cpf":"%s","precCp":"%s","dataNascimento":"%s","telefone":"%s","senha":"%s"}
                """.formatted(cpf, precCp, dataNascimento, telefone, senha);
    }

    private static String corpoValido() {
        return corpo("390.533.447-05", "458100200-01", "1968-03-15", "(12) 98765-4321", "senha1234");
    }

    private ResultActions enviar(String login, String corpo) throws Exception {
        return mockMvc.perform(post(URL)
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    // ===== sucesso =====

    @Test
    void sucesso_retorna201ComIdSemSenhaEGravaUsuarioBeneficiarioAtivoComHash() throws Exception {
        enviar(CHEFE, corpoValido())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        // CPF vira login (sem mascara), perfil BENEFICIARIO, ativo, senha so como hash
        Usuario usuario = usuarioRepository.findByLogin(CPF_NOVO).orElseThrow();
        assertThat(usuario.getPerfil()).isEqualTo(PerfilUsuario.BENEFICIARIO);
        assertThat(usuario.getAtivo()).isTrue();
        assertThat(usuario.getSenhaHash()).isNotEqualTo("senha1234");
        assertThat(passwordEncoder.matches("senha1234", usuario.getSenhaHash())).isTrue();

        // Prec CP e telefone sem mascara; so a data de nascimento e gravada (idade e faixa sao calculadas)
        Beneficiario beneficiario = beneficiarioRepository.findById(usuario.getIdUsuario()).orElseThrow();
        assertThat(beneficiario.getNome()).isEqualTo("Marta Souza Lima");
        assertThat(beneficiario.getPrecCp()).isEqualTo("45810020001");
        assertThat(beneficiario.getTelefone()).isEqualTo("12987654321");
        assertThat(beneficiario.getDataNascimento()).isEqualTo(LocalDate.of(1968, 3, 15));
    }

    // ===== 422 =====

    @Test
    void cpfInvalido_retorna422NoCampoCpf() throws Exception {
        enviar(CHEFE, corpo("123.456.789-00", "45810020001", "1968-03-15", "12987654321", "senha1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF invalido"));
        assertThat(usuarioRepository.findByLogin("12345678900")).isEmpty();
    }

    @Test
    void dataFutura_retorna422NoCampoDataNascimento() throws Exception {
        String amanha = Beneficiario.hoje().plusDays(1).toString();
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", amanha, "12987654321", "senha1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("dataNascimento: A data de nascimento deve estar no passado"));
        // hoje tambem nao e passado
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", Beneficiario.hoje().toString(), "12987654321", "senha1234"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(usuarioRepository.findByLogin(CPF_NOVO)).isEmpty();
    }

    @Test
    void dataEmFormatoInvalido_retorna422NoCampoDataNascimento() throws Exception {
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", "15/03/1968", "12987654321", "senha1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("dataNascimento:")));
    }

    @Test
    void telefoneInvalido_retorna422NoCampoTelefone() throws Exception {
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", "1968-03-15", "1234", "senha1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("telefone:")));
    }

    @Test
    void senhaCurta_retorna422NoCampoSenha() throws Exception {
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", "1968-03-15", "12987654321", "1234567"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("senha:")));
    }

    @Test
    void nomeEmBranco_retorna422NoCampoNome() throws Exception {
        enviar(CHEFE, """
                {"nome":" ","cpf":"39053344705","precCp":"45810020001","dataNascimento":"1968-03-15","telefone":"12987654321","senha":"senha1234"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("nome: Campo obrigatorio"));
    }

    // ===== 409 =====

    @Test
    void cpfRepetido_retorna409IndicandoCpf() throws Exception {
        enviar(CHEFE, corpo(CPF_BENEFICIARIO_EXISTENTE, "45810020001", "1968-03-15", "12987654321", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF ja cadastrado"));
        // CPF de um medico tambem conta: o login e unico
        enviar(CHEFE, corpo(CPF_MEDICO_EXISTENTE, "45810020001", "1968-03-15", "12987654321", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF ja cadastrado"));
    }

    @Test
    void precCpRepetido_retorna409IndicandoPrecCp() throws Exception {
        enviar(CHEFE, corpo(CPF_NOVO, "458722133-00", "1968-03-15", "12987654321", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
        // Prec CP de medico tambem conta
        enviar(CHEFE, corpo(CPF_NOVO, "11122233344", "1968-03-15", "12987654321", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
        assertThat(usuarioRepository.findByLogin(CPF_NOVO)).isEmpty();
    }

    // ===== acesso =====

    @Test
    void perfilErrado_retorna403SemGravar() throws Exception {
        enviar(CPF_MEDICO_EXISTENTE, corpoValido()).andExpect(status().isForbidden());
        // perfil vem antes da validacao: corpo invalido tambem da 403
        enviar(CPF_BENEFICIARIO_EXISTENTE, corpo("123", "1", "x", "1", "1")).andExpect(status().isForbidden());

        assertThat(usuarioRepository.findByLogin(CPF_NOVO)).isEmpty();
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpoValido()))
                .andExpect(status().isUnauthorized());
    }
}
