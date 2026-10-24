package fatec.fusex.nexus.ocs;

import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class OcsBeneficiarioControllerTest {

    private static final String URL = "/beneficiarios/me/ocs";
    private static final String HEADER = SimulacaoLoginFilter.HEADER;
    private static final String LOGIN_BENEFICIARIO = "44444444444";
    private static final String LOGIN_SETOR = "setor.be3";
    private static final String LOGIN_MEDICO = "medico.be3";

    @Autowired private MockMvc mockMvc;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        criarUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        criarUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_GUIAS);
        criarUsuario(LOGIN_MEDICO, PerfilUsuario.MEDICO);

        criarOcs("Laboratório Ômega be3", "CTR-BE3-001", OcsTipo.OCS, true);
        criarOcs("Hospital Alfa be3", "CTR-BE3-002", OcsTipo.PSA, true);
        criarOcs("Antigo be3", "CTR-BE3-003", OcsTipo.OCS, false);
    }

    @Test
    @DisplayName("busca por nome ignora acento e maiusculas")
    void buscaPorNome_semAcentoEMaiusculas() throws Exception {
        mockMvc.perform(get(URL).param("busca", "LABORATORIO omega")
                        .header(HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Laboratório Ômega be3"))
                .andExpect(jsonPath("$[0].contrato").value("CTR-BE3-001"))
                .andExpect(jsonPath("$[0].tipo").value("OCS"));
    }

    @Test
    @DisplayName("busca por contrato")
    void buscaPorContrato() throws Exception {
        mockMvc.perform(get(URL).param("busca", "ctr-be3-002")
                        .header(HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Hospital Alfa be3"))
                .andExpect(jsonPath("$[0].tipo").value("PSA"));
    }

    @Test
    @DisplayName("busca por tipo")
    void buscaPorTipo() throws Exception {
        mockMvc.perform(get(URL).param("busca", "psa")
                        .header(HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", everyItem(equalTo("PSA"))))
                .andExpect(jsonPath("$[*].nome", hasItem("Hospital Alfa be3")));
    }

    @Test
    @DisplayName("sem resultado devolve lista vazia")
    void semResultado_retornaListaVazia() throws Exception {
        mockMvc.perform(get(URL).param("busca", "zzz-nao-existe-be3")
                        .header(HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("OCS inativa nao aparece na busca")
    void ocsInativa_foraDaLista() throws Exception {
        mockMvc.perform(get(URL).param("busca", "antigo be3")
                        .header(HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("resultados em ordem alfabetica por nome")
    void resultados_emOrdemAlfabetica() throws Exception {
        mockMvc.perform(get(URL).param("busca", "be3")
                        .header(HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Hospital Alfa be3"))
                .andExpect(jsonPath("$[1].nome").value("Laboratório Ômega be3"));
    }

    @Test
    @DisplayName("no maximo 30 resultados")
    void limiteDeTrintaResultados() throws Exception {
        for (int i = 1; i <= 31; i++) {
            criarOcs(String.format("Lote be3 %02d", i), String.format("CTR-LOTE-%02d", i),
                    OcsTipo.OCS, true);
        }

        mockMvc.perform(get(URL).param("busca", "lote be3")
                        .header(HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(30)));
    }

    @Test
    @DisplayName("403: perfil SETOR_GUIAS")
    void perfilSetor_retorna403() throws Exception {
        mockMvc.perform(get(URL).param("busca", "be3")
                        .header(HEADER, LOGIN_SETOR))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403: perfil MEDICO")
    void perfilMedico_retorna403() throws Exception {
        mockMvc.perform(get(URL).param("busca", "be3")
                        .header(HEADER, LOGIN_MEDICO))
                .andExpect(status().isForbidden());
    }

    // ===== helpers =====

    private void criarUsuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(perfil);
        u.setAtivo(true);
        usuarioRepository.save(u);
    }

    private void criarOcs(String nome, String contrato, OcsTipo tipo, boolean ativo) {
        Ocs ocs = new Ocs();
        ocs.setNome(nome);
        ocs.setContratoNum(contrato);
        ocs.setTipo(tipo);
        ocs.setAtivo(ativo);
        ocsRepository.save(ocs);
    }
}