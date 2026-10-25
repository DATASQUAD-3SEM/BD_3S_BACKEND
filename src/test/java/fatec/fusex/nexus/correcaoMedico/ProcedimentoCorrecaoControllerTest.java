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
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.ocsProcedimento.OcsProcedimento;
import fatec.fusex.nexus.ocsProcedimento.OcsProcedimentoId;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRolAns;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + query no H2) do GET /medicos/me/procedimentos. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ProcedimentoCorrecaoControllerTest {

    private static final String URL = "/medicos/me/procedimentos";
    private static final String MEDICO = "11144477735";
    private static final String BENEFICIARIO = "52998224725";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final ProcedimentoCorrecaoRepository procedimentoRepository;
    private final OcsRepository ocsRepository;
    private final EntityManager entityManager;

    ProcedimentoCorrecaoControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            ProcedimentoCorrecaoRepository procedimentoRepository,
            OcsRepository ocsRepository,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.ocsRepository = ocsRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void dados() {
        usuario(MEDICO, PerfilUsuario.MEDICO);
        usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO);

        ProcedimentoExame hemograma = procedimento("40304361", "Hemograma com contagem de plaquetas");
        procedimento("40302040", "Glicose");
        procedimento("40301630", "Colesterol total");

        // Hemograma ligado a uma OCS; os outros sem nenhuma: a busca nao filtra pela OCS
        Ocs ocs = new Ocs();
        ocs.setNome("Hospital Exemplo");
        ocs.setContratoNum("EXEMPLO-002");
        ocs.setTipo(OcsTipo.PSA);
        ocsRepository.saveAndFlush(ocs);
        entityManager.persist(new OcsProcedimento(
                new OcsProcedimentoId(ocs.getIdOcs(), hemograma.getIdProcedimentoExame()), ocs, hemograma));
    }

    private void usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuarioRepository.saveAndFlush(usuario);
    }

    private ProcedimentoExame procedimento(String codigoTuss, String nome) {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss(codigoTuss);
        p.setTerminologiaProcedimento(nome);
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        return procedimentoRepository.saveAndFlush(p);
    }

    private MockHttpServletRequestBuilder comoMedico(String busca) {
        return get(URL).param("busca", busca).header(SimulacaoLoginFilter.HEADER, MEDICO);
    }

    @Test
    void buscaPorNome_ignoraMaiusculasEDevolveCodigoTussENome() throws Exception {
        mockMvc.perform(comoMedico("GLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigoTuss").value("40302040"))
                .andExpect(jsonPath("$[0].nome").value("Glicose"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void buscaPorCodigo_parcial_emOrdemAlfabeticaNoCatalogoInteiro() throws Exception {
        // traz o ligado a OCS e os que nao estao em nenhuma
        mockMvc.perform(comoMedico("403"))
                .andExpect(jsonPath("$[*].nome").value(
                        contains("Colesterol total", "Glicose", "Hemograma com contagem de plaquetas")));
        mockMvc.perform(comoMedico("40304361"))
                .andExpect(jsonPath("$[*].codigoTuss").value(contains("40304361")));
    }

    @Test
    void semResultado_devolveListaVazia() throws Exception {
        mockMvc.perform(comoMedico("inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void limiteDe30() throws Exception {
        for (int i = 1; i <= 35; i++) {
            procedimento("9" + String.format("%07d", i), "Massa " + String.format("%02d", i));
        }

        mockMvc.perform(comoMedico("massa"))
                .andExpect(jsonPath("$", hasSize(30)))
                .andExpect(jsonPath("$[0].nome").value("Massa 01"));
    }

    @Test
    void termoCurtoOuAusente_retorna400() throws Exception {
        mockMvc.perform(comoMedico("a")).andExpect(status().isBadRequest());
        mockMvc.perform(comoMedico(" a ")).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, MEDICO)).andExpect(status().isBadRequest());
    }

    @Test
    void curingaDoLike_valeComoTexto() throws Exception {
        mockMvc.perform(comoMedico("%%")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void perfilErrado_retorna403MesmoComTermoCurto() throws Exception {
        mockMvc.perform(get(URL).param("busca", "glic").header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL).param("busca", "a").header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(get(URL).param("busca", "glic")).andExpect(status().isUnauthorized());
    }
}
