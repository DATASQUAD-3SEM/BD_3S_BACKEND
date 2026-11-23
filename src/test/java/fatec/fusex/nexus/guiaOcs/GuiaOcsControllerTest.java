package fatec.fusex.nexus.guiaOcs;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.ContaOcs;
import fatec.fusex.nexus.ocs.ContaOcsRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinatura.AssinaturaRepository;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de GET /ocs/guias (T19-BE1), com banco em memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaOcsControllerTest {

    private static final LocalDate PASSADA = LocalDate.of(2020, 1, 1);
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String LOGIN_OCS = "ocs.lab";
    private static final String LOGIN_OUTRA_OCS = "ocs.outra";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final ContaOcsRepository contaOcsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final AssinaturaRepository assinaturaRepository;

    private Ocs ocs;
    private Ocs outraOcs;
    private Usuario usuarioJoao;
    private Beneficiario joao;
    private Usuario setor;
    private Assinatura assinaturaJoao;
    private int sequencia;

    GuiaOcsControllerTest(MockMvc mockMvc,
                          UsuarioRepository usuarioRepository,
                          BeneficiarioRepository beneficiarioRepository,
                          OcsRepository ocsRepository,
                          ContaOcsRepository contaOcsRepository,
                          PreGuiaRepository preGuiaRepository,
                          GuiaRepository guiaRepository,
                          GuiaProcedimentoRepository procedimentoRepository,
                          SessaoRepository sessaoRepository,
                          AssinaturaRepository assinaturaRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.contaOcsRepository = contaOcsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    @BeforeEach
    void setUp() {
        ocs = ocsRepository.save(novaOcs("Laboratorio Lab", "CTR-LAB"));
        outraOcs = ocsRepository.save(novaOcs("Clinica Outra", "CTR-OUT"));

        vincularConta(LOGIN_OCS, ocs);
        vincularConta(LOGIN_OUTRA_OCS, outraOcs);

        usuarioJoao = usuario("11111111111", PerfilUsuario.BENEFICIARIO);
        joao = beneficiario(usuarioJoao, "11111111111", "Joao Logado");
        setor = usuario("setor01", PerfilUsuario.SETOR_GUIAS);

        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(usuarioJoao);
        assinatura.setAssinaturaUrl("assinaturas/joao.png");
        assinaturaJoao = assinaturaRepository.save(assinatura);
    }

    @Test
    void listaSoAsGuiasEnviadasAOcsLogada_damaisRecenteParaAMaisAntiga() throws Exception {
        criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.ENVIADA, "Envio antigo");
        Guia maisRecente = criarGuia(ocs, LocalDateTime.of(2026, 10, 20, 10, 0), FUTURA,
                SituacaoExecucao.EM_EXECUCAO, "Envio recente");
        criarGuia(outraOcs, LocalDateTime.of(2026, 10, 25, 10, 0), FUTURA, SituacaoExecucao.ENVIADA, "Outra OCS");

        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(maisRecente.getIdGuia()))
                .andExpect(jsonPath("$[0].numero").value(maisRecente.getNumero()))
                .andExpect(jsonPath("$[0].beneficiario.nome").value("Joao Logado"))
                .andExpect(jsonPath("$[0].beneficiario.precCp").value("11111111111"))
                .andExpect(jsonPath("$[0].situacao").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$[0].dataEnvio").exists());
    }

    @Test
    void naoMostraGuiaNaoEnviadaNemVencida() throws Exception {
        criarGuia(ocs, null, FUTURA, SituacaoExecucao.NAO_ENVIADA, "Nao enviada");
        criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.VENCIDA, "Vencida");
        Guia visivel = criarGuia(ocs, LocalDateTime.of(2026, 10, 2, 10, 0), FUTURA,
                SituacaoExecucao.REALIZADA, "Realizada");

        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(visivel.getIdGuia()))
                .andExpect(jsonPath("$[0].situacao").value("REALIZADA"));
    }

    @Test
    void viradaAntesDoFiltro_guiaEmExecucaoVencidaSemAssinatura_some() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA,
                SituacaoExecucao.EM_EXECUCAO, "Sem assinatura");
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        Sessao solicitada = criarSessao(exame, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertSituacao(guia, SituacaoExecucao.VENCIDA);
        assertSituacaoSessao(solicitada, SituacaoSessao.EXPIRADA);
    }

    @Test
    void viradaAntesDoFiltro_guiaVencidaComSessaoAssinada_apareceComoEncerrada() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA,
                SituacaoExecucao.EM_EXECUCAO, "Com assinatura");
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        criarSessao(exame, 1, SituacaoSessao.ASSINADA);

        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].situacao").value("ENCERRADA"));

        assertSituacao(guia, SituacaoExecucao.ENCERRADA);
    }

    @Test
    void devolveOsProcedimentosDaGuia() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA,
                SituacaoExecucao.ENVIADA, "Com exames");
        criarProcedimento(guia, "41001109", 3);
        criarProcedimento(guia, "41001110", 1);

        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].procedimentos", hasSize(2)))
                .andExpect(jsonPath("$[0].procedimentos[0].codDgp").value("41001109"))
                .andExpect(jsonPath("$[0].procedimentos[0].quantidade").value(3));
    }

    @Test
    void semGuias_devolveListaVazia() throws Exception {
        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void perfilDiferenteDeOcs_recebe403() throws Exception {
        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, "setor01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void contaOcsSemVinculo_recebe403() throws Exception {
        Usuario semVinculo = usuario("ocs.sem.vinculo", PerfilUsuario.OCS);

        mockMvc.perform(get("/ocs/guias").header(SimulacaoLoginFilter.HEADER, semVinculo.getLogin()))
                .andExpect(status().isForbidden());
    }

    // ---------- fixtures ----------

    private Ocs novaOcs(String nome, String contrato) {
        Ocs o = new Ocs();
        o.setNome(nome);
        o.setContratoNum(contrato);
        o.setTipo(OcsTipo.OCS);
        return o;
    }

    private void vincularConta(String login, Ocs alvo) {
        Usuario conta = usuario(login, PerfilUsuario.OCS);
        ContaOcs contaOcs = new ContaOcs();
        contaOcs.setUsuario(conta);
        contaOcs.setOcs(alvo);
        contaOcsRepository.save(contaOcs);
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

    /** Cria guia de Joao enviada a OCS informada (dataEnvio nula = nao enviada). */
    private Guia criarGuia(Ocs ocsDaGuia, LocalDateTime envio, LocalDate validade,
                           SituacaoExecucao situacao, String observacao) {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setBeneficiario(joao);
        preGuia.setOcs(ocsDaGuia);
        preGuia.setEncaminhamentoUrl("encaminhamento.pdf");
        preGuia = preGuiaRepository.save(preGuia);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(String.format("%09d", ++sequencia));
        guia.setArquivoUrl("guia.pdf");
        guia.setDataInsercao(LocalDateTime.of(2026, 9, 1, 9, 0));
        guia.setUsuario(setor);
        guia.setDataValidadeAte(validade);
        guia.setSituacaoExecucao(situacao);
        if (envio != null) {
            guia.setDataEnvio(envio);
            guia.setEnviadaPor(usuarioJoao);
        }
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
        org.junit.jupiter.api.Assertions.assertEquals(esperada, atual.getSituacaoExecucao());
    }

    private void assertSituacaoSessao(Sessao sessao, SituacaoSessao esperada) {
        sessaoRepository.flush();
        Sessao atual = sessaoRepository.findById(sessao.getIdSessao()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(esperada, atual.getStatus());
    }
}
