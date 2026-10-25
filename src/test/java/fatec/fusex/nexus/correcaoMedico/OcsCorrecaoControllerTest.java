package fatec.fusex.nexus.correcaoMedico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** Ponta a ponta (filtro de login + controller + service + query no H2) do GET /medicos/me/ocs. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class OcsCorrecaoControllerTest {

    private static final String URL = "/medicos/me/ocs";
    private static final String MEDICO = "11144477735";
    private static final String BENEFICIARIO = "52998224725";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final OcsCorrecaoRepository ocsRepository;

    OcsCorrecaoControllerTest(MockMvc mockMvc, UsuarioRepository usuarioRepository, OcsCorrecaoRepository ocsRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.ocsRepository = ocsRepository;
    }

    @BeforeEach
    void dados() {
        usuario(MEDICO, PerfilUsuario.MEDICO);
        usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO);

        ocs("Laboratorio Exemplo", "EXEMPLO-001", OcsTipo.OCS, true);
        ocs("Hospital Exemplo", "EXEMPLO-002", OcsTipo.PSA, true);
        ocs("Clinica Inativa Exemplo", "EXEMPLO-003", OcsTipo.OCS, false);
    }

    private void usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuarioRepository.saveAndFlush(usuario);
    }

    private void ocs(String nome, String contrato, OcsTipo tipo, boolean ativo) {
        Ocs ocs = new Ocs();
        ocs.setNome(nome);
        ocs.setContratoNum(contrato);
        ocs.setTipo(tipo);
        ocs.setAtivo(ativo);
        ocsRepository.saveAndFlush(ocs);
    }

    private MockHttpServletRequestBuilder comoMedico() {
        return get(URL).header(SimulacaoLoginFilter.HEADER, MEDICO);
    }

    @Test
    void semBusca_devolveAListaInicialDeAtivasEmOrdemAlfabeticaComOContrato() throws Exception {
        mockMvc.perform(comoMedico())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resultados[*].nome").value(contains("Hospital Exemplo", "Laboratorio Exemplo")))
                .andExpect(jsonPath("$.resultados[0].id").isNumber())
                .andExpect(jsonPath("$.resultados[0].contrato").value("EXEMPLO-002"))
                .andExpect(jsonPath("$.resultados[0].tipo").value("PSA"))
                .andExpect(jsonPath("$.resultados[0].contratoNum").doesNotExist())
                .andExpect(jsonPath("$.resultados[0].ativo").doesNotExist());
    }

    @Test
    void buscaPorNome_ignoraMaiusculas() throws Exception {
        mockMvc.perform(comoMedico().param("busca", "LABORA"))
                .andExpect(jsonPath("$.resultados[*].nome").value(contains("Laboratorio Exemplo")));
    }

    @Test
    void buscaPorContrato() throws Exception {
        mockMvc.perform(comoMedico().param("busca", "exemplo-002"))
                .andExpect(jsonPath("$.resultados[*].nome").value(contains("Hospital Exemplo")));
    }

    @Test
    void buscaPorTipo() throws Exception {
        mockMvc.perform(comoMedico().param("busca", "psa"))
                .andExpect(jsonPath("$.resultados[*].nome").value(contains("Hospital Exemplo")));
    }

    @Test
    void semResultado_devolveVazioComTotalZero() throws Exception {
        mockMvc.perform(comoMedico().param("busca", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.resultados", hasSize(0)));
    }

    @Test
    void totalMaiorQueOLimite_devolve30EOTotalGeral() throws Exception {
        for (int i = 1; i <= 35; i++) {
            ocs("Massa " + String.format("%02d", i), "MASSA-" + i, OcsTipo.OCS, true);
        }

        mockMvc.perform(comoMedico().param("busca", "massa"))
                .andExpect(jsonPath("$.resultados", hasSize(30)))
                .andExpect(jsonPath("$.total").value(35))
                .andExpect(jsonPath("$.resultados[0].nome").value("Massa 01"));
    }

    @Test
    void ocsInativa_ficaForaDaBuscaEDaListaInicial() throws Exception {
        mockMvc.perform(comoMedico().param("busca", "inativa"))
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(comoMedico().param("busca", "EXEMPLO-003"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void curingaDoLike_valeComoTexto() throws Exception {
        mockMvc.perform(comoMedico().param("busca", "%"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void perfilErrado_retorna403() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
