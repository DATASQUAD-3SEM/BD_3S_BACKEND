package fatec.fusex.nexus.chefeFusex;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** Ponta a ponta (filtro de login + controller + service + query no H2) do GET /chefe/medicos. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeMedicoControllerTest {

    private static final String URL = "/chefe/medicos";
    private static final String CHEFE = "chefe.teste";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MedicoFusexRepository medicoRepository;

    @BeforeEach
    void dados() {
        usuario(CHEFE, PerfilUsuario.CHEFE_FUSEX, true);
        usuario("52998224725", PerfilUsuario.BENEFICIARIO, true);

        // nomes fora de ordem de proposito, para conferir a ordem alfabetica
        medico("98765432100", "Marta Souza Lima", "45810020001", "654321-RJ", true);
        medico("11144477735", "Carlos Eduardo Lima", "11122233344", "123456-SP", true);
        medico("22233344455", "Bruno Alves Costa", "45830040002", "777888-MG", false);
    }

    private Usuario usuario(String login, PerfilUsuario perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void medico(String cpf, String nome, String precCp, String crm, boolean ativo) {
        MedicoFusex m = new MedicoFusex();
        m.setUsuario(usuario(cpf, PerfilUsuario.MEDICO, ativo));
        m.setNome(nome);
        m.setPrecCp(precCp);
        m.setCrm(crm);
        m.setPatente("Capitao");
        medicoRepository.saveAndFlush(m);
    }

    private MockHttpServletRequestBuilder comoChefe() {
        return get(URL).header(SimulacaoLoginFilter.HEADER, CHEFE);
    }

    // ===== situacao =====

    @Test
    void semSituacao_usaAtivosComoPadrao_emOrdemAlfabetica() throws Exception {
        mockMvc.perform(comoChefe())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Carlos Eduardo Lima", "Marta Souza Lima")));
    }

    @Test
    void situacaoAtivos_trazSoAtivosComOsCamposDoContrato() throws Exception {
        mockMvc.perform(comoChefe().param("situacao", "ativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].id").isNumber())
                .andExpect(jsonPath("$.itens[0].nome").value("Carlos Eduardo Lima"))
                .andExpect(jsonPath("$.itens[0].cpf").value("11144477735"))
                .andExpect(jsonPath("$.itens[0].precCp").value("11122233344"))
                .andExpect(jsonPath("$.itens[0].crm").value("123456-SP"))
                .andExpect(jsonPath("$.itens[0].ativo").value(true))
                .andExpect(jsonPath("$.itens[0].patente").doesNotExist())
                .andExpect(jsonPath("$.itens[0].senhaHash").doesNotExist());
    }

    @Test
    void situacaoInativos_trazSoInativos() throws Exception {
        mockMvc.perform(comoChefe().param("situacao", "inativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Bruno Alves Costa"))
                .andExpect(jsonPath("$.itens[0].ativo").value(false));
    }

    @Test
    void situacaoTodos_trazAtivosEInativos() throws Exception {
        mockMvc.perform(comoChefe().param("situacao", "todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.itens[*].nome").value(
                        contains("Bruno Alves Costa", "Carlos Eduardo Lima", "Marta Souza Lima")));
    }

    @Test
    void situacaoInvalida_retorna400() throws Exception {
        mockMvc.perform(comoChefe().param("situacao", "excluidos"))
                .andExpect(status().isBadRequest());
    }

    // ===== busca =====

    @Test
    void buscaPorNome_ignoraMaiusculas() throws Exception {
        mockMvc.perform(comoChefe().param("busca", "MARTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Marta Souza Lima")));
    }

    @Test
    void buscaPorCpf_comESemMascara() throws Exception {
        mockMvc.perform(comoChefe().param("busca", "11144477735"))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Carlos Eduardo Lima")));
        mockMvc.perform(comoChefe().param("busca", "111.444.777-35"))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Carlos Eduardo Lima")));
    }

    @Test
    void buscaPorPrecCp_comESemMascara() throws Exception {
        mockMvc.perform(comoChefe().param("busca", "45810020001"))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Marta Souza Lima")));
        mockMvc.perform(comoChefe().param("busca", "458100200-01"))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Marta Souza Lima")));
    }

    @Test
    void buscaPorCrm_inteiroOuParcialSemDiferenciarMaiusculas() throws Exception {
        mockMvc.perform(comoChefe().param("busca", "654321-rj"))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Marta Souza Lima")));
        mockMvc.perform(comoChefe().param("busca", "-SP"))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Carlos Eduardo Lima")));
    }

    @Test
    void buscaRespeitaASituacao() throws Exception {
        // Bruno e inativo: nao aparece no padrao (ativos), aparece em inativos
        mockMvc.perform(comoChefe().param("busca", "bruno"))
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(comoChefe().param("busca", "bruno").param("situacao", "inativos"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void buscaSemResultado_devolveListaVazia() throws Exception {
        mockMvc.perform(comoChefe().param("busca", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    // ===== paginacao =====

    @Test
    void paginacao_respeitaPageESizeEMantemOTotal() throws Exception {
        mockMvc.perform(comoChefe().param("situacao", "todos").param("page", "0").param("size", "2"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Bruno Alves Costa", "Carlos Eduardo Lima")));

        mockMvc.perform(comoChefe().param("situacao", "todos").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Marta Souza Lima")));
    }

    @Test
    void paginacaoInvalida_retorna400() throws Exception {
        mockMvc.perform(comoChefe().param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(comoChefe().param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(comoChefe().param("size", "101")).andExpect(status().isBadRequest());
    }

    // ===== acesso =====

    @Test
    void perfilErrado_retorna403() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, "11144477735"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, "52998224725"))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilErradoComSituacaoInvalida_retorna403EnaoRevelaValidacao() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, "11144477735").param("situacao", "xyz"))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
