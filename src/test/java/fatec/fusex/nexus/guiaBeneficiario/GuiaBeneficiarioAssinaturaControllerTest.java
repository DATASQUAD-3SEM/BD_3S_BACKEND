package fatec.fusex.nexus.guiaBeneficiario;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinatura.AssinaturaRepository;
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaBeneficiarioAssinaturaControllerTest {

    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final LocalDateTime INSERCAO = LocalDateTime.of(2026, 10, 20, 10, 0);

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final AssinaturaRepository assinaturaRepository;

    private Usuario usuarioJoao;
    private Beneficiario joao;
    private Beneficiario maria;
    private Usuario setor;
    private Ocs ocs;
    private Assinatura assinaturaJoao;
    private int sequencia;

    GuiaBeneficiarioAssinaturaControllerTest(MockMvc mockMvc,
                                             UsuarioRepository usuarioRepository,
                                             BeneficiarioRepository beneficiarioRepository,
                                             OcsRepository ocsRepository,
                                             PreGuiaRepository preGuiaRepository,
                                             GuiaRepository guiaRepository,
                                             GuiaProcedimentoRepository procedimentoRepository,
                                             SessaoRepository sessaoRepository,
                                             AssinaturaRepository assinaturaRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    @BeforeEach
    void setUp() {
        usuarioJoao = usuario("11111111111", PerfilUsuario.BENEFICIARIO);
        joao = beneficiario(usuarioJoao, "11111111111", "Joao Logado");

        Usuario usuarioMaria = usuario("22222222222", PerfilUsuario.BENEFICIARIO);
        maria = beneficiario(usuarioMaria, "22222222222", "Maria Outra");

        setor = usuario("setor01", PerfilUsuario.SETOR_GUIAS);

        ocs = new Ocs();
        ocs.setNome("Hospital Central");
        ocs.setContratoNum("123456789");
        ocs.setTipo(OcsTipo.OCS);
        ocs = ocsRepository.save(ocs);

        assinaturaJoao = new Assinatura();
        assinaturaJoao.setUsuario(usuarioJoao);
        assinaturaJoao.setAssinaturaUrl("assinaturas/joao.png");
        assinaturaJoao = assinaturaRepository.save(assinaturaJoao);

        // Vincula a assinatura ao usuario (o exigirPerfil le esse campo no assinar).
        usuarioJoao.setAssinatura(assinaturaJoao);
        usuarioJoao = usuarioRepository.save(usuarioJoao);
    }

    @Test
    void assinar_caminhoFeliz_gravaStatusDataEAssinatura() throws Exception {
        Guia guia = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc = criarProcedimento(guia, "41001109", 2);
        Sessao sessao1 = criarSessao(proc, 1, SituacaoSessao.SOLICITADA);
        criarSessao(proc, 2, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia.getIdGuia(), sessao1.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.sessoes[0].status").value("ASSINADA"));

        sessaoRepository.flush();
        Sessao atual = sessaoRepository.findById(sessao1.getIdSessao()).orElseThrow();
        Assertions.assertEquals(SituacaoSessao.ASSINADA, atual.getStatus());
        Assertions.assertNotNull(atual.getDataAssinatura());
        Assertions.assertEquals(usuarioJoao.getIdUsuario(), atual.getAssinadoPor().getIdUsuario());
        Assertions.assertEquals(assinaturaJoao.getIdAssinatura(), atual.getAssinatura().getIdAssinatura());
    }

    @Test
    void assinar_sessaoJaAssinada_recebe409() throws Exception {
        Guia guia = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc = criarProcedimento(guia, "41001109", 2);
        Sessao sessao = criarSessao(proc, 1, SituacaoSessao.ASSINADA);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia.getIdGuia(), sessao.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A sessao nao esta aguardando assinatura"));
    }

    @Test
    void assinar_semAssinaturaCadastrada_recebe422_sessaoContinuaSolicitada() throws Exception {
        // Remove a assinatura do usuario logado
        usuarioJoao.setAssinatura(null);
        usuarioRepository.saveAndFlush(usuarioJoao);

        Guia guia = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc = criarProcedimento(guia, "41001109", 1);
        Sessao sessao = criarSessao(proc, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia.getIdGuia(), sessao.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(
                        "assinatura: Você ainda não tem assinatura cadastrada. Cadastre em Minhas informações para assinar."));

        sessaoRepository.flush();
        Sessao atual = sessaoRepository.findById(sessao.getIdSessao()).orElseThrow();
        Assertions.assertEquals(SituacaoSessao.SOLICITADA, atual.getStatus());
        Assertions.assertNull(atual.getDataAssinatura());
    }

    @Test
    void assinar_regra25JaAtingida_recebe409() throws Exception {
        // quantidade=1, mas forcamos 1 ASSINADA + 1 SOLICITADA (dado inconsistente de proposito).
        Guia guia = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc = criarProcedimento(guia, "41001109", 1);
        criarSessao(proc, 1, SituacaoSessao.ASSINADA);
        Sessao sessaoSolicitada = criarSessao(proc, 2, SituacaoSessao.SOLICITADA);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia.getIdGuia(), sessaoSolicitada.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Todas as sessoes autorizadas deste procedimento ja foram assinadas"));

        sessaoRepository.flush();
        Sessao atual = sessaoRepository.findById(sessaoSolicitada.getIdSessao()).orElseThrow();
        Assertions.assertEquals(SituacaoSessao.SOLICITADA, atual.getStatus());
    }

    @Test
    void assinar_guiaDeOutroBeneficiario_recebe403() throws Exception {
        Guia guiaDaMaria = criarGuia(maria, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc = criarProcedimento(guiaDaMaria, "41001109", 1);
        Sessao sessao = criarSessao(proc, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guiaDaMaria.getIdGuia(), sessao.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assinar_sessaoDeOutraGuia_recebe404() throws Exception {
        Guia guia1 = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc1 = criarProcedimento(guia1, "41001109", 1);
        criarSessao(proc1, 1, SituacaoSessao.SOLICITADA);

        Guia guia2 = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc2 = criarProcedimento(guia2, "41001109", 1);
        Sessao sessaoDaGuia2 = criarSessao(proc2, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia1.getIdGuia(), sessaoDaGuia2.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assinar_guiaInexistente_recebe404() throws Exception {
        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura", 999999L, 1L)
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assinar_sessaoInexistente_recebe404() throws Exception {
        Guia guia = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        criarProcedimento(guia, "41001109", 1);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia.getIdGuia(), 999999L)
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assinar_perfilSetorGuias_recebe403() throws Exception {
        Guia guia = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc = criarProcedimento(guia, "41001109", 1);
        Sessao sessao = criarSessao(proc, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia.getIdGuia(), sessao.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "setor01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assinar_ultimaSessao_situacaoViraRealizada() throws Exception {
        Guia guia = criarGuia(joao, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento proc = criarProcedimento(guia, "41001109", 1);
        Sessao sessao = criarSessao(proc, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(post("/guias/{id}/sessoes/{sessaoId}/assinatura",
                        guia.getIdGuia(), sessao.getIdSessao())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("REALIZADA"));

        guiaRepository.flush();
        Guia atual = guiaRepository.findById(guia.getIdGuia()).orElseThrow();
        Assertions.assertEquals(SituacaoExecucao.REALIZADA, atual.getSituacaoExecucao());
    }

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

    private Guia criarGuia(Beneficiario dono, SituacaoExecucao situacao) {
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
        guia.setDataInsercao(INSERCAO);
        guia.setUsuario(setor);
        guia.setDataValidadeAte(FUTURA);
        guia.setSituacaoExecucao(situacao);
        return guiaRepository.save(guia);
    }

    private GuiaProcedimento criarProcedimento(Guia guia, String codDgp, int quantidade) {
        GuiaProcedimento p = new GuiaProcedimento();
        p.setGuia(guia);
        p.setCodDgp(codDgp);
        p.setDescricaoProcedimento("TC - Abdome superior");
        p.setQuantidadeAutorizada(quantidade);
        return procedimentoRepository.save(p);
    }

    private Sessao criarSessao(GuiaProcedimento procedimento, int numero, SituacaoSessao status) {
        Sessao s = new Sessao();
        s.setGuiaProcedimento(procedimento);
        s.setNumero(numero);
        s.setStatus(status);
        if (status != SituacaoSessao.DISPONIVEL) {
            s.setDataSolicitacao(LocalDateTime.of(2026, 10, 2, 9, 0));
            s.setSolicitadaPor(usuarioJoao);
        }
        if (status == SituacaoSessao.ASSINADA) {
            s.setDataAssinatura(LocalDateTime.of(2026, 10, 2, 10, 0));
            s.setAssinadoPor(usuarioJoao);
            s.setAssinatura(assinaturaJoao);
        }
        return sessaoRepository.save(s);
    }
}