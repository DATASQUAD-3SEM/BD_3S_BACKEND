package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimento;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoId;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoRepository;
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
    // T16-BE3: validade conferida no rascunho, copiada para a Guia na conclusao
    private static final LocalDate VALIDADE_CONFERIDA = LocalDate.of(2099, 1, 1);

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;
    private final GuiaRascunhoProcedimentoRepository guiaRascunhoProcedimentoRepository;
    private final GuiaProcedimentoRepository guiaProcedimentoRepository;
    private final EntityManager entityManager;

    private Usuario setor;
    private Beneficiario beneficiario;
    private Ocs ocs;

    ConcluirGuiaControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            GuiaRepository guiaRepository,
            GuiaRascunhoRepository guiaRascunhoRepository,
            GuiaRascunhoProcedimentoRepository guiaRascunhoProcedimentoRepository,
            GuiaProcedimentoRepository guiaProcedimentoRepository,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
        this.guiaRascunhoProcedimentoRepository = guiaRascunhoProcedimentoRepository;
        this.guiaProcedimentoRepository = guiaProcedimentoRepository;
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

    private PreGuia preGuia(PreGuiaStatus status) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(beneficiario);
        p.setOcs(ocs);
        p.setStatus(status);
        p.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        return preGuiaRepository.saveAndFlush(p);
    }

    // O rascunho e semeado direto no repositorio (como pede a task), sem passar pelo T4-BE5.
    // Com dois exames, como o setor deixaria conferidos.
    private void rascunho(PreGuia preGuia, String numero) {
        rascunhoSemExames(preGuia, numero);
        guiaRascunhoProcedimentoRepository.saveAllAndFlush(List.of(
                new GuiaRascunhoProcedimento(new GuiaRascunhoProcedimentoId(preGuia.getIdPreGuia(), 1),
                        "41001109", "TC - Abdome superior", 1),
                new GuiaRascunhoProcedimento(new GuiaRascunhoProcedimentoId(preGuia.getIdPreGuia(), 2),
                        "50011015", "Consulta pediatra", 2)));
    }

    private void rascunhoSemExames(PreGuia preGuia, String numero) {
        criarRascunho(preGuia, numero, VALIDADE_CONFERIDA);
    }

    // T16-BE3: validade null = rascunho gravado antes da T16-BE2
    private void criarRascunho(PreGuia preGuia, String numero, LocalDate validade) {
        GuiaRascunho r = new GuiaRascunho();
        r.setPreGuia(preGuia);
        r.setNumero(numero);
        r.setArquivoUrl("guias-rascunho/" + numero + ".pdf");
        r.setNomeArquivo("guia-" + numero + ".pdf");
        r.setDataAtualizacao(LocalDateTime.now());
        r.setValidadeAte(validade);
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
                .andExpect(jsonPath("$.validadeAte").value(VALIDADE_CONFERIDA.toString()))
                .andExpect(jsonPath("$.assinaturasPendentes").value(contains("MEDICO", "CHEFE")));

        entityManager.flush();
        entityManager.clear();
        Guia guia = guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia()).orElseThrow();
        assertThat(guia.getNumero()).isEqualTo("2026001234");
        assertThat(guia.getArquivoUrl()).isEqualTo("guias-rascunho/2026001234.pdf");
        // T16-BE3: a validade do rascunho (NAO RECEBER APOS) vai para a Guia, sem calculo pela insercao
        assertThat(guia.getDataValidadeAte()).isEqualTo(VALIDADE_CONFERIDA);
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

    // T22-BE2: os exames do rascunho viram GuiaProcedimento (o V.Unit nao e guardado, T28-BE3)
    @Test
    void sucesso_copiaOsExamesDoRascunhoParaAGuia() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        rascunho(p, "2026001234");

        concluir(SETOR, p.getIdPreGuia()).andExpect(status().isCreated());

        entityManager.flush();
        entityManager.clear();
        Long guiaId = guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia()).orElseThrow().getIdGuia();
        assertThat(guiaProcedimentoRepository.findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(guiaId)))
                .extracting(gp -> gp.getCodDgp(), gp -> gp.getDescricaoProcedimento(),
                        gp -> gp.getQuantidadeAutorizada())
                .containsExactly(tuple("41001109", "TC - Abdome superior", 1),
                        tuple("50011015", "Consulta pediatra", 2));
    }

    // T16-BE3: rascunho gravado antes da T16-BE2 nao tem validade: 409 e nenhuma Guia criada
    @Test
    void rascunhoSemValidade_retorna409SemCriarGuia() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        criarRascunho(p, "2026001234", null);

        concluir(SETOR, p.getIdPreGuia())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Guia sem validade conferida"));

        assertThat(guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia())).isEmpty();
        assertThat(guiaProcedimentoRepository.count()).isZero();
    }

    @Test
    void rascunhoSemExames_retorna409SemCriarGuiaNemExames() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        rascunhoSemExames(p, "2026001234");

        concluir(SETOR, p.getIdPreGuia())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Exames da Guia não informados"));

        assertThat(guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia())).isEmpty();
        assertThat(guiaProcedimentoRepository.count()).isZero();
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
        assertThat(guiaProcedimentoRepository.count()).isEqualTo(2);
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
