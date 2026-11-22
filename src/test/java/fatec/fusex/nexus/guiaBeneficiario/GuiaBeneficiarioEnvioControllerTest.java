package fatec.fusex.nexus.guiaBeneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
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
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/**
 * Testes de POST /guias/{id}/envio (T18-BE-2): envio da guia pronta à OCS, criando as sessões.
 * Fica separado do GuiaBeneficiarioControllerTest (T18-BE-1), seguindo o padrão do projeto de
 * um arquivo de teste por endpoint/task.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaBeneficiarioEnvioControllerTest {

    private static final String BENEFICIARIO = "52998224725";
    private static final String OUTRO_BENEFICIARIO = "39053344705";
    private static final String SETOR = "setor.teste";
    private static final LocalDate VALIDADE_FUTURA = LocalDate.of(2099, 1, 1);

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final EntityManager entityManager;

    private Usuario usuarioLogado;
    private Beneficiario beneficiario;
    private Beneficiario outroBeneficiario;
    private Usuario setor;
    private Ocs ocs;
    private int sequencia;

    GuiaBeneficiarioEnvioControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            GuiaRepository guiaRepository,
            GuiaProcedimentoRepository procedimentoRepository,
            SessaoRepository sessaoRepository,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        usuarioLogado = usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        beneficiario = beneficiario(usuarioLogado, "11111111111", "Joao Logado");

        Usuario usuarioOutro = usuario(OUTRO_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        outroBeneficiario = beneficiario(usuarioOutro, "22222222222", "Maria Outra");

        setor = usuario(SETOR, PerfilUsuario.SETOR_GUIAS);

        ocs = new Ocs();
        ocs.setNome("Hospital Central");
        ocs.setContratoNum("CTR-001");
        ocs.setTipo(OcsTipo.OCS);
        ocs = ocsRepository.save(ocs);
    }

    //sucesso
    @Test
    void sucesso_criaExatamenteAsSessoesAutorizadas_todasDisponivelNumeradas() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 2);
        criarProcedimento(guia, "41001110", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ENVIADA"))
                .andExpect(jsonPath("$.sessoes", hasSize(3)))
                .andExpect(jsonPath("$.sessoes[0].status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.sessoes[1].status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.sessoes[2].status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.sessoes[0].numero").value(1))
                .andExpect(jsonPath("$.sessoes[1].numero").value(2))
                .andExpect(jsonPath("$.sessoes[2].numero").value(1));

        List<Sessao> sessoes = sessoesDaGuia(guia.getIdGuia());
        assertThat(sessoes).hasSize(3);
        assertThat(sessoes).allSatisfy(s -> {
            assertThat(s.getStatus()).isEqualTo(SituacaoSessao.DISPONIVEL);
            assertThat(s.getDataSolicitacao()).isNull();
            assertThat(s.getDataAssinatura()).isNull();
            assertThat(s.getAssinadoPor()).isNull();
            assertThat(s.getAssinatura()).isNull();
        });
    }

    @Test
    void sucesso_gravaDataEnvioEEnviadaPor() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ENVIADA"))
                .andExpect(jsonPath("$.dataEnvio", notNullValue()));

        Guia noBanco = recarregar(guia.getIdGuia());
        assertThat(noBanco.getDataEnvio()).isNotNull();
        assertThat(noBanco.getEnviadaPor()).isNotNull();
        assertThat(noBanco.getEnviadaPor().getIdUsuario()).isEqualTo(usuarioLogado.getIdUsuario());
    }

    @Test
    void sucesso_devolveProcedimentosESessoesDaGuiaAtualizada() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guia.getIdGuia()))
                .andExpect(jsonPath("$.ocs").value("Hospital Central"))
                .andExpect(jsonPath("$.validadeAte").value("2099-01-01"))
                .andExpect(jsonPath("$.procedimentos", hasSize(1)))
                .andExpect(jsonPath("$.procedimentos[0].codDgp").value("41001109"))
                .andExpect(jsonPath("$.procedimentos[0].quantidade").value(1));
    }

    // 409 
    @Test
    void guiaAindaSemAsDuasAssinaturas_retorna409_semAlterar_semCriarSessoes() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 1);
        guia.getPreGuia().setStatus(PreGuiaStatus.EM_ANALISE);
        preGuiaRepository.save(guia.getPreGuia());

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem")
                        .value("A Guia ainda não foi assinada pelo médico e pelo chefe do FUSEX"));

        Guia noBanco = recarregar(guia.getIdGuia());
        assertThat(noBanco.getSituacaoExecucao()).isEqualTo(SituacaoExecucao.NAO_ENVIADA);
        assertThat(noBanco.getDataEnvio()).isNull();
        assertThat(sessoesDaGuia(guia.getIdGuia())).isEmpty();
    }

    @Test
    void guiaSemValidade_retorna409_semAlterar_semCriarSessoes() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, null);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Guia sem validade"));

        Guia noBanco = recarregar(guia.getIdGuia());
        assertThat(noBanco.getSituacaoExecucao()).isEqualTo(SituacaoExecucao.NAO_ENVIADA);
        assertThat(noBanco.getDataEnvio()).isNull();
        assertThat(sessoesDaGuia(guia.getIdGuia())).isEmpty();
    }

    @Test
    void guiaSemExames_retorna409_semAlterar_semCriarSessoes() throws Exception {
        // Sem nenhum GuiaProcedimento: acontece em guia concluída antes da T22.
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Guia sem exames cadastrados"));

        Guia noBanco = recarregar(guia.getIdGuia());
        assertThat(noBanco.getSituacaoExecucao()).isEqualTo(SituacaoExecucao.NAO_ENVIADA);
        assertThat(noBanco.getDataEnvio()).isNull();
        assertThat(sessoesDaGuia(guia.getIdGuia())).isEmpty();
    }

    @Test
    void guiaJaEnviada_retorna409_semCriarSessoesDeNovo() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Esta guia ja foi enviada à OCS"));

        assertThat(sessoesDaGuia(guia.getIdGuia())).isEmpty();
    }

    // 403 / 404 / 401
    @Test
    void guiaDeOutroBeneficiario_retorna403_semAlterar_semCriarSessoes() throws Exception {
        Guia guia = criarGuia(outroBeneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isForbidden());

        Guia noBanco = recarregar(guia.getIdGuia());
        assertThat(noBanco.getSituacaoExecucao()).isEqualTo(SituacaoExecucao.NAO_ENVIADA);
        assertThat(sessoesDaGuia(guia.getIdGuia())).isEmpty();
    }

    @Test
    void guiaInexistente_retorna404() throws Exception {
        mockMvc.perform(post("/guias/999999/envio")
                        .header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilDiferenteDeBeneficiario_retorna403_semAlterar() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio")
                        .header(SimulacaoLoginFilter.HEADER, SETOR))
                .andExpect(status().isForbidden());

        assertThat(recarregar(guia.getIdGuia()).getSituacaoExecucao())
                .isEqualTo(SituacaoExecucao.NAO_ENVIADA);
        assertThat(sessoesDaGuia(guia.getIdGuia())).isEmpty();
    }

    @Test
    void semLogin_retorna401() throws Exception {
        Guia guia = criarGuia(beneficiario, SituacaoExecucao.NAO_ENVIADA, VALIDADE_FUTURA);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/" + guia.getIdGuia() + "/envio"))
                .andExpect(status().isUnauthorized());
    }

    // fixtures
    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(perfil);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    private Beneficiario beneficiario(Usuario usuario, String precCp, String nome) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1990, 1, 1));
        b.setTelefone("11999999991");
        return beneficiarioRepository.save(b);
    }

    private Guia criarGuia(Beneficiario dono, SituacaoExecucao situacao, LocalDate validadeAte) {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setBeneficiario(dono);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamento.pdf");
        preGuia = preGuiaRepository.save(preGuia);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(String.format("%09d", ++sequencia));
        guia.setArquivoUrl("guia.pdf");
        guia.setDataInsercao(LocalDateTime.now());
        guia.setUsuario(setor);
        guia.setDataValidadeAte(validadeAte);
        guia.setSituacaoExecucao(situacao);
        return guiaRepository.save(guia);
    }

    private GuiaProcedimento criarProcedimento(Guia guia, String codDgp, int quantidade) {
        GuiaProcedimento p = new GuiaProcedimento();
        p.setGuia(guia);
        p.setCodDgp(codDgp);
        p.setDescricaoProcedimento("Procedimento " + codDgp);
        p.setQuantidadeAutorizada(quantidade);
        return procedimentoRepository.save(p);
    }

    private Guia recarregar(Long idGuia) {
        entityManager.flush();
        entityManager.clear();
        return guiaRepository.findById(idGuia).orElseThrow();
    }

    private List<Sessao> sessoesDaGuia(Long idGuia) {
        entityManager.flush();
        return sessaoRepository
                .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                        List.of(idGuia));
    }
}