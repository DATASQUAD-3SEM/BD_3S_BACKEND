package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2) do POST /setor-guias/pre-guias/{id}/guia/concluir. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ConcluirGuiaControllerTest {

    private static final String SETOR = "setor.teste";
    private static final String BENEFICIARIO = "52998224725";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private GuiaRascunhoRepository guiaRascunhoRepository;
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

    private PreGuia preGuia(PreGuiaStatus status) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(beneficiario);
        p.setOcs(ocs);
        p.setStatus(status);
        p.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        return preGuiaRepository.saveAndFlush(p);
    }

    // O rascunho e semeado direto no repositorio (como pede a task), sem passar pelo T4-BE5
    private void rascunho(PreGuia preGuia, String numero) {
        GuiaRascunho r = new GuiaRascunho();
        r.setPreGuia(preGuia);
        r.setNumero(numero);
        r.setArquivoUrl("guias-rascunho/" + numero + ".pdf");
        r.setNomeArquivo("guia-" + numero + ".pdf");
        r.setDataAtualizacao(LocalDateTime.now());
        guiaRascunhoRepository.saveAndFlush(r);
    }

    private ResultActions concluir(String login, Long id) throws Exception {
        return mockMvc.perform(post("/setor-guias/pre-guias/" + id + "/guia/concluir")
                .header(SimulacaoLoginFilter.HEADER, login));
    }

    @Test
    void sucesso_criaAGuiaComOPdfDoRascunhoPendenteDasDuasAssinaturas() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        rascunho(p, "2026001234");

        concluir(SETOR, p.getIdPreGuia())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guiaId").isNumber())
                .andExpect(jsonPath("$.numero").value("2026001234"))
                .andExpect(jsonPath("$.validadeAte").value(LocalDate.now().plusMonths(2).toString()))
                .andExpect(jsonPath("$.assinaturasPendentes").value(contains("MEDICO", "CHEFE")));

        entityManager.flush();
        entityManager.clear();
        Guia guia = guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia()).orElseThrow();
        assertThat(guia.getNumero()).isEqualTo("2026001234");
        assertThat(guia.getArquivoUrl()).isEqualTo("guias-rascunho/2026001234.pdf");
        assertThat(guia.getUsuario().getIdUsuario()).isEqualTo(setor.getIdUsuario());
        // a pre-guia continua Em analise ate as duas assinaturas
        assertThat(preGuiaRepository.findById(p.getIdPreGuia()).orElseThrow().getStatus())
                .isEqualTo(PreGuiaStatus.EM_ANALISE);
        // e a Guia entra na lista de Guias para assinar dos dois papeis (T5-BE1)
        assertThat(guiaRepository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO))
                .extracting(Guia::getNumero).contains("2026001234");
        assertThat(guiaRepository.buscarSemAssinatura(AssinaturaGuiaPapel.CHEFE))
                .extracting(Guia::getNumero).contains("2026001234");
    }

    @Test
    void semRascunho_retorna409GuiaNaoAnexada() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);

        concluir(SETOR, p.getIdPreGuia())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Guia nao anexada"));

        assertThat(guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia())).isEmpty();
    }

    @Test
    void guiaJaCriada_oSegundoEnvioRetorna409() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        rascunho(p, "2026001234");
        concluir(SETOR, p.getIdPreGuia()).andExpect(status().isCreated());

        concluir(SETOR, p.getIdPreGuia())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A Guia desta pre-guia ja foi criada"));

        assertThat(guiaRepository.count()).isEqualTo(1);
    }

    @Test
    void statusQueNaoPermite_retorna409() throws Exception {
        for (PreGuiaStatus status : new PreGuiaStatus[] {PreGuiaStatus.PENDENTE, PreGuiaStatus.ATENCAO, PreGuiaStatus.CONCLUIDA}) {
            PreGuia p = preGuia(status);
            rascunho(p, "20260" + status.ordinal());
            concluir(SETOR, p.getIdPreGuia()).andExpect(status().isConflict());
            assertThat(guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia())).isEmpty();
        }
    }

    @Test
    void inexistente_retorna404() throws Exception {
        concluir(SETOR, 999999L).andExpect(status().isNotFound());
    }

    @Test
    void perfilErrado_retorna403SemCriarGuia() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        rascunho(p, "2026001234");

        concluir(BENEFICIARIO, p.getIdPreGuia()).andExpect(status().isForbidden());

        assertThat(guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia())).isEmpty();
    }

    @Test
    void semLogin_retorna401() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        mockMvc.perform(post("/setor-guias/pre-guias/" + p.getIdPreGuia() + "/guia/concluir"))
                .andExpect(status().isUnauthorized());
    }
}
