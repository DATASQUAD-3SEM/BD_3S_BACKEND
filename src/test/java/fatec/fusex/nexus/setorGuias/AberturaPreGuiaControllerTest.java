package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

/** Ponta a ponta (filtro de login + controller + service + H2) do POST /setor-guias/pre-guias/{id}/abertura. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AberturaPreGuiaControllerTest {

    private static final String SETOR = "setor.teste";
    private static final String BENEFICIARIO = "52998224725";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final EntityManager entityManager;

    private Usuario setor;
    private Beneficiario beneficiario;
    private Ocs ocs;

    AberturaPreGuiaControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            PreGuiaHistoricoRepository historicoRepository,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
        this.entityManager = entityManager;
    }

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

    private ResultActions abrir(String login, Long id) throws Exception {
        return mockMvc.perform(post("/setor-guias/pre-guias/" + id + "/abertura")
                .header(SimulacaoLoginFilter.HEADER, login));
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
    void pendente_viraEmAnaliseERegistraOSetorNoHistorico() throws Exception {
        Long id = preGuia(PreGuiaStatus.PENDENTE);

        abrir(SETOR, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));

        assertThat(statusNoBanco(id)).isEqualTo(PreGuiaStatus.EM_ANALISE);
        List<PreGuiaHistorico> historico = historicoDe(id);
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getStatus()).isEqualTo(PreGuiaStatus.EM_ANALISE);
        assertThat(historico.get(0).getUsuario().getIdUsuario()).isEqualTo(setor.getIdUsuario());
        assertThat(historico.get(0).getDataHora()).isNotNull();
    }

    @Test
    void emAnalise_permaneceSemNovaLinhaDeHistorico() throws Exception {
        Long id = preGuia(PreGuiaStatus.PENDENTE);
        abrir(SETOR, id).andExpect(status().isOk());

        // abrir de novo (ex.: recarregar a tela) e idempotente
        abrir(SETOR, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));

        assertThat(historicoDe(id)).hasSize(1);
    }

    @Test
    void atencaoEConcluida_permanecemSemAlterarNada() throws Exception {
        Long emAtencao = preGuia(PreGuiaStatus.ATENCAO);
        Long concluida = preGuia(PreGuiaStatus.CONCLUIDA);

        abrir(SETOR, emAtencao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENCAO"));
        abrir(SETOR, concluida)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        assertThat(statusNoBanco(emAtencao)).isEqualTo(PreGuiaStatus.ATENCAO);
        assertThat(statusNoBanco(concluida)).isEqualTo(PreGuiaStatus.CONCLUIDA);
        assertThat(historicoDe(emAtencao)).isEmpty();
        assertThat(historicoDe(concluida)).isEmpty();
    }

    @Test
    void inexistente_retorna404() throws Exception {
        abrir(SETOR, 999999L).andExpect(status().isNotFound());
    }

    @Test
    void perfilErrado_retorna403SemAlterar() throws Exception {
        Long id = preGuia(PreGuiaStatus.PENDENTE);

        abrir(BENEFICIARIO, id).andExpect(status().isForbidden());

        assertThat(statusNoBanco(id)).isEqualTo(PreGuiaStatus.PENDENTE);
    }

    @Test
    void semLogin_retorna401() throws Exception {
        Long id = preGuia(PreGuiaStatus.PENDENTE);
        mockMvc.perform(post("/setor-guias/pre-guias/" + id + "/abertura"))
                .andExpect(status().isUnauthorized());
    }
}
