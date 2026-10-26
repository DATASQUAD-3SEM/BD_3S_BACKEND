package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2) do GET e PUT /chefe/medicos/{id}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeMedicoEdicaoControllerTest {

    private static final String CHEFE = "chefe.teste";
    private static final String HASH_ORIGINAL = "hash-original";

    // CPFs validos (digitos verificadores conferem)
    private static final String CPF_MEDICO = "11144477735";
    private static final String CPF_OUTRO_MEDICO = "39053344705";
    private static final String CPF_BENEFICIARIO = "52998224725";
    private static final String CPF_NOVO = "12345678909";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    private Long idMedico;

    ChefeMedicoEdicaoControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            MedicoFusexRepository medicoRepository,
            BeneficiarioRepository beneficiarioRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void dados() {
        usuario(CHEFE, PerfilUsuario.CHEFE_FUSEX);
        idMedico = medico(CPF_MEDICO, "Carlos Eduardo Lima", "11122233344", "123456-SP");
        medico(CPF_OUTRO_MEDICO, "Marta Souza Lima", "45810020001", "654321-RJ");

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(CPF_BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash(HASH_ORIGINAL);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Long medico(String cpf, String nome, String precCp, String crm) {
        MedicoFusex m = new MedicoFusex();
        m.setUsuario(usuario(cpf, PerfilUsuario.MEDICO));
        m.setNome(nome);
        m.setPatente("Capitao");
        m.setPrecCp(precCp);
        m.setCrm(crm);
        return medicoRepository.saveAndFlush(m).getUsuarioId();
    }

    private static String corpo(String cpf, String precCp, String crm, boolean ativo, String novaSenha) {
        String senha = novaSenha == null ? "null" : "\"" + novaSenha + "\"";
        return """
                {"nome":"Carlos Eduardo Lima Junior","patente":"Major","cpf":"%s","precCp":"%s","crm":"%s","ativo":%s,"novaSenha":%s}
                """.formatted(cpf, precCp, crm, ativo, senha);
    }

    private ResultActions editar(String login, Long id, String corpo) throws Exception {
        return mockMvc.perform(put("/chefe/medicos/" + id)
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private MedicoFusex recarregar() {
        entityManager.flush();
        entityManager.clear();
        return medicoRepository.findWithUsuarioByUsuarioId(idMedico).orElseThrow();
    }

    // ===== GET =====

    @Test
    void get_devolveOsDadosDoMedicoSemSenha() throws Exception {
        mockMvc.perform(get("/chefe/medicos/" + idMedico).header(SimulacaoLoginFilter.HEADER, CHEFE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idMedico))
                .andExpect(jsonPath("$.nome").value("Carlos Eduardo Lima"))
                .andExpect(jsonPath("$.patente").value("Capitao"))
                .andExpect(jsonPath("$.cpf").value(CPF_MEDICO))
                .andExpect(jsonPath("$.precCp").value("11122233344"))
                .andExpect(jsonPath("$.crm").value("123456-SP"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void get_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/chefe/medicos/999999").header(SimulacaoLoginFilter.HEADER, CHEFE))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_idDeBeneficiario_retorna404() throws Exception {
        Long idBeneficiario = usuarioRepository.findByLogin(CPF_BENEFICIARIO).orElseThrow().getIdUsuario();
        mockMvc.perform(get("/chefe/medicos/" + idBeneficiario).header(SimulacaoLoginFilter.HEADER, CHEFE))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_perfilErrado_retorna403() throws Exception {
        mockMvc.perform(get("/chefe/medicos/" + idMedico).header(SimulacaoLoginFilter.HEADER, CPF_MEDICO))
                .andExpect(status().isForbidden());
    }

    // ===== PUT: sucesso =====

    @Test
    void put_sucesso_alteraTodosOsCamposEMantemASenhaSemNovaSenha() throws Exception {
        editar(CHEFE, idMedico, corpo("123.456.789-09", "458300400-02", "777888/mg", true, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Eduardo Lima Junior"))
                .andExpect(jsonPath("$.patente").value("Major"))
                .andExpect(jsonPath("$.cpf").value(CPF_NOVO))
                .andExpect(jsonPath("$.precCp").value("45830040002"))
                .andExpect(jsonPath("$.crm").value("777888-MG"))
                .andExpect(jsonPath("$.ativo").value(true));

        MedicoFusex medico = recarregar();
        assertThat(medico.getUsuario().getLogin()).isEqualTo(CPF_NOVO); // CPF novo vira o login
        assertThat(medico.getCrm()).isEqualTo("777888-MG");
        assertThat(medico.getUsuario().getSenhaHash()).isEqualTo(HASH_ORIGINAL);
    }

    @Test
    void put_mantendoOsProprioCpfPrecCpECrm_naoEDuplicidade() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "123456-SP", true, ""))
                .andExpect(status().isOk());
    }

    @Test
    void put_novaSenhaVazia_mantemASenhaAtual() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "123456-SP", true, ""))
                .andExpect(status().isOk());

        assertThat(recarregar().getUsuario().getSenhaHash()).isEqualTo(HASH_ORIGINAL);
    }

    @Test
    void put_novaSenhaValida_gravaComHash() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "123456-SP", true, "novasenha123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novaSenha").doesNotExist());

        String hash = recarregar().getUsuario().getSenhaHash();
        assertThat(hash).isNotEqualTo("novasenha123");
        assertThat(passwordEncoder.matches("novasenha123", hash)).isTrue();
    }

    @Test
    void put_mudancaDeSituacao_desativaEReativa() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "123456-SP", false, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        assertThat(recarregar().getUsuario().getAtivo()).isFalse();

        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "123456-SP", true, null))
                .andExpect(jsonPath("$.ativo").value(true));
        assertThat(recarregar().getUsuario().getAtivo()).isTrue();
    }

    // ===== PUT: 422 =====

    @Test
    void put_novaSenhaCurta_retorna422NoCampoNovaSenha() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "123456-SP", true, "1234567"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("novaSenha:")));
    }

    @Test
    void put_mesmasValidacoesDoCadastro_retorna422() throws Exception {
        editar(CHEFE, idMedico, corpo("123.456.789-00", "11122233344", "123456-SP", true, null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF invalido"));
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "ABC", true, null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("crm:")));
    }

    @Test
    void put_semAtivo_retorna422NoCampoAtivo() throws Exception {
        editar(CHEFE, idMedico, """
                {"nome":"Carlos","patente":"Major","cpf":"11144477735","precCp":"11122233344","crm":"123456-SP"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("ativo: Campo obrigatorio"));
    }

    // ===== PUT: 409 =====

    @Test
    void put_cpfDeOutroUsuario_retorna409IndicandoCpf() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_OUTRO_MEDICO, "11122233344", "123456-SP", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF ja cadastrado"));
        editar(CHEFE, idMedico, corpo(CPF_BENEFICIARIO, "11122233344", "123456-SP", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF ja cadastrado"));
    }

    @Test
    void put_precCpDeOutroUsuario_retorna409IndicandoPrecCp() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "45810020001", "123456-SP", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "45872213300", "123456-SP", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
    }

    @Test
    void put_crmDeOutroMedico_retorna409IndicandoCrm() throws Exception {
        editar(CHEFE, idMedico, corpo(CPF_MEDICO, "11122233344", "654321/rj", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("crm: CRM ja cadastrado"));
        assertThat(recarregar().getCrm()).isEqualTo("123456-SP");
    }

    // ===== PUT: 404, 403 e 401 =====

    @Test
    void put_inexistente_retorna404() throws Exception {
        editar(CHEFE, 999999L, corpo(CPF_NOVO, "45830040002", "777888-MG", true, null))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_perfilErrado_retorna403SemAlterar() throws Exception {
        editar(CPF_MEDICO, idMedico, corpo(CPF_MEDICO, "11122233344", "123456-SP", false, null))
                .andExpect(status().isForbidden());

        assertThat(recarregar().getUsuario().getAtivo()).isTrue();
    }

    @Test
    void put_semLogin_retorna401() throws Exception {
        mockMvc.perform(put("/chefe/medicos/" + idMedico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(CPF_MEDICO, "11122233344", "123456-SP", true, null)))
                .andExpect(status().isUnauthorized());
    }
}
