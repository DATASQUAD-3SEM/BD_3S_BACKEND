package fatec.fusex.nexus.chefeBeneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2) do PATCH /chefe/beneficiarios/{id}/situacao. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeBeneficiarioSituacaoControllerTest {

    private static final String CHEFE = "chefe.teste";
    private static final String CPF_BENEFICIARIO = "52998224725";
    private static final String CPF_MEDICO = "11144477735";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final EntityManager entityManager;

    private Long idBeneficiario;
    private Long idPreGuia;

    ChefeBeneficiarioSituacaoControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void dados() {
        usuario(CHEFE, PerfilUsuario.CHEFE_FUSEX);
        usuario(CPF_MEDICO, PerfilUsuario.MEDICO);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(CPF_BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        idBeneficiario = beneficiarioRepository.saveAndFlush(beneficiario).getUsuarioId();

        // Historico do beneficiario: a pre-guia precisa continuar ligada a ele depois de desativar
        Ocs ocs = new Ocs();
        ocs.setNome("Laboratorio Exemplo");
        ocs.setContratoNum("EXEMPLO-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.saveAndFlush(ocs);
        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        idPreGuia = preGuiaRepository.saveAndFlush(preGuia).getIdPreGuia();
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
        return mockMvc.perform(patch("/chefe/beneficiarios/" + id + "/situacao")
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private Boolean ativoNoBanco() {
        entityManager.flush();
        entityManager.clear();
        return usuarioRepository.findByLogin(CPF_BENEFICIARIO).orElseThrow().getAtivo();
    }

    @Test
    void desativar_retorna200EGravaInativoSemApagarNadaDoHistorico() throws Exception {
        alterar(CHEFE, idBeneficiario, "{\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idBeneficiario))
                .andExpect(jsonPath("$.ativo").value(false));

        assertThat(ativoNoBanco()).isFalse();
        // nunca DELETE: beneficiario, usuario e a pre-guia continuam no banco
        assertThat(beneficiarioRepository.findById(idBeneficiario)).isPresent();
        assertThat(usuarioRepository.findByLogin(CPF_BENEFICIARIO)).isPresent();
        assertThat(preGuiaRepository.findById(idPreGuia)).isPresent()
                .get().extracting(p -> p.getBeneficiario().getUsuarioId()).isEqualTo(idBeneficiario);
    }

    @Test
    void reativar_retorna200EGravaAtivo() throws Exception {
        alterar(CHEFE, idBeneficiario, "{\"ativo\":false}").andExpect(status().isOk());

        alterar(CHEFE, idBeneficiario, "{\"ativo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void repetirAMesmaSituacao_eIdempotenteERetorna200() throws Exception {
        alterar(CHEFE, idBeneficiario, "{\"ativo\":false}").andExpect(status().isOk());
        alterar(CHEFE, idBeneficiario, "{\"ativo\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        assertThat(ativoNoBanco()).isFalse();

        // ja ativo -> ativar de novo tambem e 200
        alterar(CHEFE, idBeneficiario, "{\"ativo\":true}").andExpect(status().isOk());
        alterar(CHEFE, idBeneficiario, "{\"ativo\":true}").andExpect(status().isOk());
        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void inexistente_retorna404() throws Exception {
        alterar(CHEFE, 999999L, "{\"ativo\":false}").andExpect(status().isNotFound());
    }

    @Test
    void idDeUsuarioQueNaoEBeneficiario_retorna404() throws Exception {
        Long idMedico = usuarioRepository.findByLogin(CPF_MEDICO).orElseThrow().getIdUsuario();
        alterar(CHEFE, idMedico, "{\"ativo\":false}").andExpect(status().isNotFound());
    }

    @Test
    void semAtivo_retorna422NoCampoAtivo() throws Exception {
        alterar(CHEFE, idBeneficiario, "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("ativo: Campo obrigatorio"));
        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void perfilErrado_retorna403SemAlterar() throws Exception {
        alterar(CPF_MEDICO, idBeneficiario, "{\"ativo\":false}").andExpect(status().isForbidden());
        alterar(CPF_BENEFICIARIO, idBeneficiario, "{\"ativo\":false}").andExpect(status().isForbidden());

        assertThat(ativoNoBanco()).isTrue();
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(patch("/chefe/beneficiarios/" + idBeneficiario + "/situacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
