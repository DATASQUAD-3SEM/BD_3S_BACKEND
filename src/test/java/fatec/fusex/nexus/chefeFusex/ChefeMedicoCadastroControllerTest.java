package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

/** Ponta a ponta (filtro de login + controller + service + H2) do POST /chefe/medicos. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeMedicoCadastroControllerTest {

    private static final String URL = "/chefe/medicos";
    private static final String CHEFE = "chefe.teste";

    // CPFs validos (digitos verificadores conferem)
    private static final String CPF_NOVO = "39053344705";
    private static final String CPF_MEDICO_EXISTENTE = "11144477735";
    private static final String CPF_BENEFICIARIO_EXISTENTE = "52998224725";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MedicoFusexRepository medicoRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void dados() {
        usuario(CHEFE, PerfilUsuario.CHEFE_FUSEX);

        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario(CPF_MEDICO_EXISTENTE, PerfilUsuario.MEDICO));
        medico.setNome("Carlos Eduardo Lima");
        medico.setPatente("Capitao");
        medico.setPrecCp("11122233344");
        medico.setCrm("123456-SP");
        medicoRepository.saveAndFlush(medico);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(CPF_BENEFICIARIO_EXISTENTE, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private static String corpo(String cpf, String precCp, String crm, String senha) {
        return """
                {"nome":"Marta Souza Lima","patente":"Major","cpf":"%s","precCp":"%s","crm":"%s","senha":"%s"}
                """.formatted(cpf, precCp, crm, senha);
    }

    private static String corpoValido() {
        return corpo("390.533.447-05", "458100200-01", "654321-rj", "senha1234");
    }

    private ResultActions enviar(String login, String corpo) throws Exception {
        return mockMvc.perform(post(URL)
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    // ===== sucesso =====

    @Test
    void sucesso_retorna201ComIdSemSenhaEGravaUsuarioMedicoAtivoComHash() throws Exception {
        enviar(CHEFE, corpoValido())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        // CPF vira login (sem mascara), perfil MEDICO, ativo, senha so como hash
        Usuario usuario = usuarioRepository.findByLogin(CPF_NOVO).orElseThrow();
        assertThat(usuario.getPerfil()).isEqualTo(PerfilUsuario.MEDICO);
        assertThat(usuario.getAtivo()).isTrue();
        assertThat(usuario.getSenhaHash()).isNotEqualTo("senha1234");
        assertThat(passwordEncoder.matches("senha1234", usuario.getSenhaHash())).isTrue();

        // Prec CP sem mascara e CRM normalizado para "654321-RJ"
        MedicoFusex medico = medicoRepository.findByUsuarioLogin(CPF_NOVO).orElseThrow();
        assertThat(medico.getNome()).isEqualTo("Marta Souza Lima");
        assertThat(medico.getPatente()).isEqualTo("Major");
        assertThat(medico.getPrecCp()).isEqualTo("45810020001");
        assertThat(medico.getCrm()).isEqualTo("654321-RJ");
    }

    // ===== 422 =====

    @Test
    void cpfInvalido_retorna422NoCampoCpf() throws Exception {
        enviar(CHEFE, corpo("123.456.789-00", "45810020001", "654321-RJ", "senha1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF invalido"));
        assertThat(usuarioRepository.findByLogin("12345678900")).isEmpty();
    }

    @Test
    void crmInvalido_retorna422NoCampoCrm() throws Exception {
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", "ABC", "senha1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.startsWith("crm:")));
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", "", "senha1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("crm: Campo obrigatorio"));
    }

    @Test
    void senhaCurta_retorna422NoCampoSenha() throws Exception {
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", "654321-RJ", "1234567"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.startsWith("senha:")));
    }

    @Test
    void nomeEmBranco_retorna422NoCampoNome() throws Exception {
        enviar(CHEFE, """
                {"nome":" ","patente":"Major","cpf":"39053344705","precCp":"45810020001","crm":"654321-RJ","senha":"senha1234"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("nome: Campo obrigatorio"));
    }

    // ===== 409 =====

    @Test
    void cpfDuplicado_retorna409IndicandoCpf() throws Exception {
        // vale para qualquer usuario: aqui o CPF e de um beneficiario
        enviar(CHEFE, corpo(CPF_BENEFICIARIO_EXISTENTE, "45810020001", "654321-RJ", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF ja cadastrado"));
    }

    @Test
    void precCpDuplicado_retorna409IndicandoPrecCp() throws Exception {
        enviar(CHEFE, corpo(CPF_NOVO, "111.222.333-44", "654321-RJ", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
        // Prec CP de beneficiario tambem conta
        enviar(CHEFE, corpo(CPF_NOVO, "45872213300", "654321-RJ", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
    }

    @Test
    void crmDuplicado_retorna409IndicandoCrm() throws Exception {
        // "123456/sp" normaliza para "123456-SP", que ja existe
        enviar(CHEFE, corpo(CPF_NOVO, "45810020001", "123456/sp", "senha1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("crm: CRM ja cadastrado"));
        assertThat(usuarioRepository.findByLogin(CPF_NOVO)).isEmpty();
    }

    // ===== acesso =====

    @Test
    void perfilErrado_retorna403SemGravar() throws Exception {
        enviar(CPF_MEDICO_EXISTENTE, corpoValido()).andExpect(status().isForbidden());
        // perfil vem antes da validacao: corpo invalido tambem da 403
        enviar(CPF_BENEFICIARIO_EXISTENTE, corpo("123", "1", "x", "1")).andExpect(status().isForbidden());

        assertThat(usuarioRepository.findByLogin(CPF_NOVO)).isEmpty();
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpoValido()))
                .andExpect(status().isUnauthorized());
    }
}
