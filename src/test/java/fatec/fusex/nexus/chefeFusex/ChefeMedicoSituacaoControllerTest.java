package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2) do PATCH /chefe/medicos/{id}/situacao. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeMedicoSituacaoControllerTest {

    private static final String CHEFE = "chefe.teste";
    private static final String CPF_MEDICO = "11144477735";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final EntityManager entityManager;

    private Long idMedico;

    ChefeMedicoSituacaoControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            MedicoFusexRepository medicoRepository,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void dados() {
        usuario(CHEFE, PerfilUsuario.CHEFE_FUSEX);

        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario(CPF_MEDICO, PerfilUsuario.MEDICO));
        medico.setNome("Carlos Eduardo Lima");
        medico.setPatente("Capitao");
        medico.setPrecCp("11122233344");
        medico.setCrm("123456-SP");
        idMedico = medicoRepository.saveAndFlush(medico).getUsuarioId();
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private ResultActions alterar(String login, Long id, String corpo) throws Exception {
        return mockMvc.perform(patch("/chefe/medicos/" + id + "/situacao")
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private Boolean ativoNoBanco() {
        entityManager.flush();
        entityManager.clear();
        return usuarioRepository.findByLogin(CPF_MEDICO).orElseThrow().getAtivo();
    }

    @Test
    void desativar_retorna200EGravaInativoSemApagarOMedico() throws Exception {
        alterar(CHEFE, idMedico, "{\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idMedico))
                .andExpect(jsonPath("$.ativo").value(false));

        assertThat(ativoNoBanco()).isFalse();
        // nunca DELETE: medico e usuario continuam no banco (historico preservado)
        assertThat(medicoRepository.findById(idMedico)).isPresent();
        assertThat(usuarioRepository.findByLogin(CPF_MEDICO)).isPresent();
    }

    @Test
    void reativar_retorna200EGravaAtivo() throws Exception {
        alterar(CHEFE, idMedico, "{\"ativo\":false}").andExpect(status().isOk());

        alterar(CHEFE, idMedico, "{\"ativo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void repetirAMesmaSituacao_eIdempotenteERetorna200() throws Exception {
        alterar(CHEFE, idMedico, "{\"ativo\":false}").andExpect(status().isOk());
        alterar(CHEFE, idMedico, "{\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        assertThat(ativoNoBanco()).isFalse();

        // ja ativo -> ativar de novo tambem e 200
        alterar(CHEFE, idMedico, "{\"ativo\":true}").andExpect(status().isOk());
        alterar(CHEFE, idMedico, "{\"ativo\":true}").andExpect(status().isOk());
        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void inexistente_retorna404() throws Exception {
        alterar(CHEFE, 999999L, "{\"ativo\":false}").andExpect(status().isNotFound());
    }

    @Test
    void idDeUsuarioQueNaoEMedico_retorna404() throws Exception {
        Long idChefe = usuarioRepository.findByLogin(CHEFE).orElseThrow().getIdUsuario();
        alterar(CHEFE, idChefe, "{\"ativo\":false}").andExpect(status().isNotFound());
    }

    @Test
    void semAtivo_retorna422NoCampoAtivo() throws Exception {
        alterar(CHEFE, idMedico, "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("ativo: Campo obrigatorio"));
        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void perfilErrado_retorna403SemAlterar() throws Exception {
        alterar(CPF_MEDICO, idMedico, "{\"ativo\":false}").andExpect(status().isForbidden());

        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(patch("/chefe/medicos/" + idMedico + "/situacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
