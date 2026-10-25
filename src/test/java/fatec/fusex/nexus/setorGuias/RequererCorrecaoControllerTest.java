package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2) do POST /setor-guias/pre-guias/{id}/correcao. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class RequererCorrecaoControllerTest {

    private static final String SETOR = "setor.teste";
    private static final String BENEFICIARIO = "52998224725";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private PreGuiaHistoricoRepository historicoRepository;
    @Autowired private EntityManager entityManager;

    private Usuario setor;
    private Beneficiario beneficiario;
    private Ocs ocs;

    @BeforeEach
    void dados() {
        setor = usuario(SETOR, PerfilUsuario.SETOR_GUIAS);

        beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);

        ocs = new Ocs();
        ocs.setNome("Laboratorio Exemplo");
        ocs.setContratoNum("EXEMPLO-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.saveAndFlush(ocs);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Long preGuia(PreGuiaStatus status) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(beneficiario);
        p.setOcs(ocs);
        p.setStatus(status);
        p.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        return preGuiaRepository.saveAndFlush(p).getIdPreGuia();
    }

    private ResultActions requerer(String login, Long id, String corpo) throws Exception {
        return mockMvc.perform(post("/setor-guias/pre-guias/" + id + "/correcao")
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private PreGuiaStatus statusNoBanco(Long id) {
        entityManager.flush();
        entityManager.clear();
        return preGuiaRepository.findById(id).orElseThrow().getStatus();
    }

    private List<PreGuiaHistorico> historicoDe(Long id) {
        entityManager.flush();
        entityManager.clear();
        return historicoRepository.findAll().stream()
                .filter(h -> h.getPreGuia().getIdPreGuia().equals(id))
                .toList();
    }

    @Test
    void sucesso_viraAtencaoEGuardaObservacaoAtorEData() throws Exception {
        Long id = preGuia(PreGuiaStatus.EM_ANALISE);

        requerer(SETOR, id, "{\"observacao\":\"  Foto desfocada, envie novamente.  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENCAO"))
                .andExpect(jsonPath("$.dataDecisao").exists());

        assertThat(statusNoBanco(id)).isEqualTo(PreGuiaStatus.ATENCAO);
        List<PreGuiaHistorico> historico = historicoDe(id);
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
        assertThat(historico.get(0).getObservacao()).isEqualTo("Foto desfocada, envie novamente.");
        assertThat(historico.get(0).getUsuario().getIdUsuario()).isEqualTo(setor.getIdUsuario());
        assertThat(historico.get(0).getDataHora()).isNotNull();
    }

    @Test
    void observacaoVaziaOuSoEspacos_retorna422SemAlterar() throws Exception {
        Long id = preGuia(PreGuiaStatus.EM_ANALISE);

        requerer(SETOR, id, "{\"observacao\":\"   \"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("observacao: Informe a observacao"));
        requerer(SETOR, id, "{}").andExpect(status().isUnprocessableEntity());

        assertThat(statusNoBanco(id)).isEqualTo(PreGuiaStatus.EM_ANALISE);
        assertThat(historicoDe(id)).isEmpty();
    }

    @Test
    void observacaoLonga_retorna422() throws Exception {
        Long id = preGuia(PreGuiaStatus.EM_ANALISE);
        String longa = "a".repeat(501);

        requerer(SETOR, id, "{\"observacao\":\"" + longa + "\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("observacao: A observacao deve ter no maximo 500 caracteres"));

        // exatamente 500 passa
        requerer(SETOR, id, "{\"observacao\":\"" + "a".repeat(500) + "\"}").andExpect(status().isOk());
    }

    @Test
    void statusQueNaoPermite_retorna409() throws Exception {
        for (PreGuiaStatus status : new PreGuiaStatus[] {PreGuiaStatus.PENDENTE, PreGuiaStatus.ATENCAO, PreGuiaStatus.CONCLUIDA}) {
            Long id = preGuia(status);
            requerer(SETOR, id, "{\"observacao\":\"Foto desfocada\"}").andExpect(status().isConflict());
            assertThat(statusNoBanco(id)).isEqualTo(status);
        }
    }

    @Test
    void inexistente_retorna404() throws Exception {
        requerer(SETOR, 999999L, "{\"observacao\":\"Foto desfocada\"}").andExpect(status().isNotFound());
    }

    @Test
    void perfilErrado_retorna403SemAlterar() throws Exception {
        Long id = preGuia(PreGuiaStatus.EM_ANALISE);

        requerer(BENEFICIARIO, id, "{\"observacao\":\"Foto desfocada\"}").andExpect(status().isForbidden());

        assertThat(statusNoBanco(id)).isEqualTo(PreGuiaStatus.EM_ANALISE);
    }

    @Test
    void semLogin_retorna401() throws Exception {
        Long id = preGuia(PreGuiaStatus.EM_ANALISE);
        mockMvc.perform(post("/setor-guias/pre-guias/" + id + "/correcao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Foto desfocada\"}"))
                .andExpect(status().isUnauthorized());
    }
}
