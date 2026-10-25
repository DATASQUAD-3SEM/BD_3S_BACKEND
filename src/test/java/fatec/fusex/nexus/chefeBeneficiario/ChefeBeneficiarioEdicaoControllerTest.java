package fatec.fusex.nexus.chefeBeneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2) do GET e PUT /chefe/beneficiarios/{id}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeBeneficiarioEdicaoControllerTest {

    private static final String CHEFE = "chefe.teste";
    private static final String HASH_ORIGINAL = "hash-original";

    // CPFs validos (digitos verificadores conferem)
    private static final String CPF_BENEFICIARIO = "52998224725";
    private static final String CPF_OUTRO_BENEFICIARIO = "39053344705";
    private static final String CPF_MEDICO = "11144477735";
    private static final String CPF_NOVO = "12345678909";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private MedicoFusexRepository medicoRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Long idBeneficiario;

    @BeforeEach
    void dados() {
        usuario(CHEFE, PerfilUsuario.CHEFE_FUSEX);
        idBeneficiario = beneficiario(CPF_BENEFICIARIO, "Antonio Carlos Ferreira", "45872213300");
        beneficiario(CPF_OUTRO_BENEFICIARIO, "Marta Souza Lima", "45810020001");

        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario(CPF_MEDICO, PerfilUsuario.MEDICO));
        medico.setNome("Carlos Eduardo Lima");
        medico.setPatente("Capitao");
        medico.setPrecCp("11122233344");
        medico.setCrm("123456-SP");
        medicoRepository.saveAndFlush(medico);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash(HASH_ORIGINAL);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Long beneficiario(String cpf, String nome, String precCp) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario(cpf, PerfilUsuario.BENEFICIARIO));
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("12987654321");
        return beneficiarioRepository.saveAndFlush(b).getUsuarioId();
    }

    // Corpo com os dados atuais do beneficiario; cada teste troca so o que precisa
    private static String corpo(String cpf, String precCp, String dataNascimento, boolean ativo, String novaSenha) {
        String senha = novaSenha == null ? "null" : "\"" + novaSenha + "\"";
        return """
                {"nome":"Antonio Carlos Ferreira Junior","cpf":"%s","precCp":"%s","dataNascimento":"%s",
                 "telefone":"(12) 99876-5432","ativo":%s,"novaSenha":%s}
                """.formatted(cpf, precCp, dataNascimento, ativo, senha);
    }

    private static String corpoMantendo(boolean ativo, String novaSenha) {
        return corpo(CPF_BENEFICIARIO, "45872213300", "1968-03-15", ativo, novaSenha);
    }

    private ResultActions editar(String login, Long id, String corpo) throws Exception {
        return mockMvc.perform(put("/chefe/beneficiarios/" + id)
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private Beneficiario recarregar() {
        entityManager.flush();
        entityManager.clear();
        return beneficiarioRepository.findWithUsuarioByUsuarioId(idBeneficiario).orElseThrow();
    }

    // ===== GET =====

    @Test
    void get_devolveOsDadosComFaixaEtariaCalculadaESemSenha() throws Exception {
        int idade = LocalDate.of(1968, 3, 15).until(Beneficiario.hoje()).getYears();

        mockMvc.perform(get("/chefe/beneficiarios/" + idBeneficiario).header(SimulacaoLoginFilter.HEADER, CHEFE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idBeneficiario))
                .andExpect(jsonPath("$.nome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$.cpf").value(CPF_BENEFICIARIO))
                .andExpect(jsonPath("$.precCp").value("45872213300"))
                .andExpect(jsonPath("$.dataNascimento").value("1968-03-15"))
                .andExpect(jsonPath("$.telefone").value("12987654321"))
                .andExpect(jsonPath("$.idade").value(idade))
                .andExpect(jsonPath("$.faixaEtaria").value(FaixaEtaria.de(idade).getRotulo()))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void get_inexistenteOuIdDeMedico_retorna404() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios/999999").header(SimulacaoLoginFilter.HEADER, CHEFE))
                .andExpect(status().isNotFound());
        Long idMedico = usuarioRepository.findByLogin(CPF_MEDICO).orElseThrow().getIdUsuario();
        mockMvc.perform(get("/chefe/beneficiarios/" + idMedico).header(SimulacaoLoginFilter.HEADER, CHEFE))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_perfilErrado_retorna403() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios/" + idBeneficiario).header(SimulacaoLoginFilter.HEADER, CPF_MEDICO))
                .andExpect(status().isForbidden());
    }

    // ===== PUT: sucesso =====

    @Test
    void put_sucesso_alteraTodosOsCamposEMantemASenhaSemNovaSenha() throws Exception {
        editar(CHEFE, idBeneficiario, corpo("123.456.789-09", "458300400-02", "1970-05-20", true, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Antonio Carlos Ferreira Junior"))
                .andExpect(jsonPath("$.cpf").value(CPF_NOVO))
                .andExpect(jsonPath("$.precCp").value("45830040002"))
                .andExpect(jsonPath("$.dataNascimento").value("1970-05-20"))
                .andExpect(jsonPath("$.telefone").value("12998765432"))
                .andExpect(jsonPath("$.ativo").value(true));

        Beneficiario b = recarregar();
        assertThat(b.getUsuario().getLogin()).isEqualTo(CPF_NOVO); // CPF novo vira o login
        assertThat(b.getDataNascimento()).isEqualTo(LocalDate.of(1970, 5, 20));
        assertThat(b.getUsuario().getSenhaHash()).isEqualTo(HASH_ORIGINAL);
    }

    @Test
    void put_mantendoOProprioCpfEPrecCp_naoEDuplicidade() throws Exception {
        editar(CHEFE, idBeneficiario, corpoMantendo(true, null)).andExpect(status().isOk());
    }

    @Test
    void put_novaSenhaVazia_mantemASenhaAtual() throws Exception {
        editar(CHEFE, idBeneficiario, corpoMantendo(true, "")).andExpect(status().isOk());

        assertThat(recarregar().getUsuario().getSenhaHash()).isEqualTo(HASH_ORIGINAL);
    }

    @Test
    void put_novaSenhaValida_gravaComHash() throws Exception {
        editar(CHEFE, idBeneficiario, corpoMantendo(true, "novasenha123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novaSenha").doesNotExist());

        String hash = recarregar().getUsuario().getSenhaHash();
        assertThat(hash).isNotEqualTo("novasenha123");
        assertThat(passwordEncoder.matches("novasenha123", hash)).isTrue();
    }

    @Test
    void put_mudancaDeSituacao_desativaEReativa() throws Exception {
        editar(CHEFE, idBeneficiario, corpoMantendo(false, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        assertThat(recarregar().getUsuario().getAtivo()).isFalse();

        editar(CHEFE, idBeneficiario, corpoMantendo(true, null))
                .andExpect(jsonPath("$.ativo").value(true));
        assertThat(recarregar().getUsuario().getAtivo()).isTrue();
    }

    // ===== PUT: 422 =====

    @Test
    void put_novaSenhaCurta_retorna422NoCampoNovaSenha() throws Exception {
        editar(CHEFE, idBeneficiario, corpoMantendo(true, "1234567"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("novaSenha:")));
        assertThat(recarregar().getUsuario().getSenhaHash()).isEqualTo(HASH_ORIGINAL);
    }

    @Test
    void put_mesmasValidacoesDoCadastro_retorna422() throws Exception {
        editar(CHEFE, idBeneficiario, corpo("123.456.789-00", "45872213300", "1968-03-15", true, null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF invalido"));
        editar(CHEFE, idBeneficiario, corpo(CPF_BENEFICIARIO, "45872213300", "2099-01-01", true, null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("dataNascimento:")));
    }

    @Test
    void put_semAtivo_retorna422NoCampoAtivo() throws Exception {
        editar(CHEFE, idBeneficiario, """
                {"nome":"Antonio","cpf":"52998224725","precCp":"45872213300","dataNascimento":"1968-03-15","telefone":"12987654321"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("ativo: Campo obrigatorio"));
    }

    // ===== PUT: 409 =====

    @Test
    void put_cpfDeOutroUsuario_retorna409IndicandoCpf() throws Exception {
        editar(CHEFE, idBeneficiario, corpo(CPF_OUTRO_BENEFICIARIO, "45872213300", "1968-03-15", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF ja cadastrado"));
        editar(CHEFE, idBeneficiario, corpo(CPF_MEDICO, "45872213300", "1968-03-15", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("cpf: CPF ja cadastrado"));
        assertThat(recarregar().getUsuario().getLogin()).isEqualTo(CPF_BENEFICIARIO);
    }

    @Test
    void put_precCpDeOutroUsuario_retorna409IndicandoPrecCp() throws Exception {
        editar(CHEFE, idBeneficiario, corpo(CPF_BENEFICIARIO, "458100200-01", "1968-03-15", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
        // Prec CP de medico tambem conta
        editar(CHEFE, idBeneficiario, corpo(CPF_BENEFICIARIO, "11122233344", "1968-03-15", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("precCp: Prec CP ja cadastrado"));
        assertThat(recarregar().getPrecCp()).isEqualTo("45872213300");
    }

    // ===== PUT: 404, 403 e 401 =====

    @Test
    void put_inexistente_retorna404() throws Exception {
        editar(CHEFE, 999999L, corpo(CPF_NOVO, "45830040002", "1968-03-15", true, null))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_perfilErrado_retorna403SemAlterar() throws Exception {
        editar(CPF_MEDICO, idBeneficiario, corpoMantendo(false, null)).andExpect(status().isForbidden());

        assertThat(recarregar().getUsuario().getAtivo()).isTrue();
    }

    @Test
    void put_semLogin_retorna401() throws Exception {
        mockMvc.perform(put("/chefe/beneficiarios/" + idBeneficiario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoMantendo(true, null)))
                .andExpect(status().isUnauthorized());
    }
}
