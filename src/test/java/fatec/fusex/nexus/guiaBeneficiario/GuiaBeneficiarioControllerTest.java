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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de GET /guias/minhas (T18-BE1), com banco em memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaBeneficiarioControllerTest {

    private static final LocalDate PASSADA = LocalDate.of(2020, 1, 1);
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);

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

    GuiaBeneficiarioControllerTest(MockMvc mockMvc,
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
    }

    @Test
    void listaSoAsGuiasDoLogado_damaisRecenteParaAMaisAntiga() throws Exception {
        criarGuia(joao, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.NAO_ENVIADA);
        Guia maisRecente = criarGuia(joao, LocalDateTime.of(2026, 10, 20, 10, 0), FUTURA, SituacaoExecucao.ENVIADA);
        criarGuia(maria, LocalDateTime.of(2026, 10, 25, 10, 0), FUTURA, SituacaoExecucao.NAO_ENVIADA);

        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(maisRecente.getIdGuia()))
                .andExpect(jsonPath("$[0].ocs").value("Hospital Central"))
                .andExpect(jsonPath("$[0].validadeAte").value("2099-01-01"))
                .andExpect(jsonPath("$[0].situacao").value("ENVIADA"));
    }

    @Test
    void montaProcedimentosESessoes_comSituacaoDaSessao() throws Exception {
        Guia guia = criarGuia(joao, LocalDateTime.of(2026, 10, 20, 10, 0), FUTURA, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        criarSessao(exame, 1, SituacaoSessao.ASSINADA);
        criarSessao(exame, 2, SituacaoSessao.SOLICITADA);

        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].procedimentos", hasSize(1)))
                .andExpect(jsonPath("$[0].procedimentos[0].codDgp").value("41001109"))
                .andExpect(jsonPath("$[0].procedimentos[0].quantidade").value(2))
                .andExpect(jsonPath("$[0].sessoes", hasSize(2)))
                .andExpect(jsonPath("$[0].sessoes[0].numero").value(1))
                .andExpect(jsonPath("$[0].sessoes[0].status").value("ASSINADA"))
                .andExpect(jsonPath("$[0].sessoes[1].status").value("SOLICITADA"));
    }

    @Test
    void semGuias_devolveListaVazia() throws Exception {
        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void perfilDiferenteDeBeneficiario_recebe403() throws Exception {
        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "setor01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validadePassada_comSessaoAssinada_viraEncerrada_eExpiraAsOutras() throws Exception {
        Guia guia = criarGuia(joao, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 3);
        Sessao assinada = criarSessao(exame, 1, SituacaoSessao.ASSINADA);
        Sessao solicitada = criarSessao(exame, 2, SituacaoSessao.SOLICITADA);
        Sessao disponivel = criarSessao(exame, 3, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").value("ENCERRADA"))
                .andExpect(jsonPath("$[0].sessoes[0].status").value("ASSINADA"))
                .andExpect(jsonPath("$[0].sessoes[1].status").value("EXPIRADA"))
                .andExpect(jsonPath("$[0].sessoes[2].status").value("EXPIRADA"));

        assertSituacao(guia, SituacaoExecucao.ENCERRADA);
        assertSituacaoSessao(assinada, SituacaoSessao.ASSINADA);
        assertSituacaoSessao(solicitada, SituacaoSessao.EXPIRADA);
        assertSituacaoSessao(disponivel, SituacaoSessao.EXPIRADA);
    }

    @Test
    void validadePassada_semSessaoAssinada_viraVencida() throws Exception {
        Guia guia = criarGuia(joao, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA, SituacaoExecucao.ENVIADA);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 1);
        Sessao solicitada = criarSessao(exame, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").value("VENCIDA"))
                .andExpect(jsonPath("$[0].sessoes[0].status").value("EXPIRADA"));

        assertSituacao(guia, SituacaoExecucao.VENCIDA);
        assertSituacaoSessao(solicitada, SituacaoSessao.EXPIRADA);
    }

    @Test
    void validadeNula_naoMudaASituacao() throws Exception {
        Guia guia = criarGuia(joao, LocalDateTime.of(2026, 10, 1, 10, 0), null, SituacaoExecucao.ENVIADA);

        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].validadeAte", nullValue()))
                .andExpect(jsonPath("$[0].situacao").value("ENVIADA"));

        assertSituacao(guia, SituacaoExecucao.ENVIADA);
    }

    @Test
    void guiaRealizada_comValidadePassada_naoMudaASituacao() throws Exception {
        Guia guia = criarGuia(joao, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA, SituacaoExecucao.REALIZADA);

        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").value("REALIZADA"));

        assertSituacao(guia, SituacaoExecucao.REALIZADA);
    }

    // ---------- fixtures ----------

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

    private Guia criarGuia(Beneficiario dono, LocalDateTime insercao, LocalDate validade, SituacaoExecucao situacao) {
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
        guia.setDataInsercao(insercao);
        guia.setUsuario(setor);
        guia.setDataValidadeAte(validade);
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

    private void assertSituacao(Guia guia, SituacaoExecucao esperada) {
        guiaRepository.flush();
        Guia atual = guiaRepository.findById(guia.getIdGuia()).orElseThrow();
        Assertions.assertEquals(esperada, atual.getSituacaoExecucao());
    }

    private void assertSituacaoSessao(Sessao sessao, SituacaoSessao esperada) {
        sessaoRepository.flush();
        Sessao atual = sessaoRepository.findById(sessao.getIdSessao()).orElseThrow();
        Assertions.assertEquals(esperada, atual.getStatus());
    }
}
