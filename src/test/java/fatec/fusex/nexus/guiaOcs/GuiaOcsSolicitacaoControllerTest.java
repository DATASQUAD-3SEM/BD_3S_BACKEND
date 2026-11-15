package fatec.fusex.nexus.guiaOcs;

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
import fatec.fusex.nexus.ocs.ContaOcs;
import fatec.fusex.nexus.ocs.ContaOcsRepository;
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

/**
 * Testes de POST /ocs/guias/{id}/sessoes/{sessaoId}/solicitacao (T19-BE3), com banco em memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaOcsSolicitacaoControllerTest {

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
    private Usuario contaOcsLab;
    private Beneficiario joao;
    private Usuario setor;
    private Assinatura assinaturaJoao;
    private int sequencia;

    GuiaOcsSolicitacaoControllerTest(MockMvc mockMvc,
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

        contaOcsLab = vincularConta(LOGIN_OCS, ocs);
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
    void solicitaSessaoDisponivel_mudaParaSolicitadaEGuiaParaEmExecucao() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.ENVIADA);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        Sessao sessao = criarSessao(exame, 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(guia, sessao)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessoes[0].status").value("SOLICITADA"))
                .andExpect(jsonPath("$.sessoes[0].dataSolicitacao").exists());

        Sessao atual = sessaoRepository.findById(sessao.getIdSessao()).orElseThrow();
        Assertions.assertEquals(SituacaoSessao.SOLICITADA, atual.getStatus());
        Assertions.assertEquals(contaOcsLab.getIdUsuario(), atual.getSolicitadaPor().getIdUsuario());
        Assertions.assertEquals(SituacaoExecucao.EM_EXECUCAO, guiaRepository.findById(guia.getIdGuia()).orElseThrow()
                .getSituacaoExecucao());
    }

    @Test
    void guiaJaEmExecucao_continuaEmExecucao() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        Sessao sessao = criarSessao(exame, 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(guia, sessao)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk());

        Assertions.assertEquals(SituacaoExecucao.EM_EXECUCAO, guiaRepository.findById(guia.getIdGuia()).orElseThrow()
                .getSituacaoExecucao());
    }

    @Test
    void mesmaSessaoDeNovo_recebe409() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.ENVIADA);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        Sessao sessao = criarSessao(exame, 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(guia, sessao)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk());
        mockMvc.perform(post(url(guia, sessao)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isConflict());
    }

    @Test
    void sessaoAssinada_recebe409() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        Sessao sessao = criarSessao(exame, 1, SituacaoSessao.ASSINADA);

        mockMvc.perform(post(url(guia, sessao)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isConflict());
    }

    @Test
    void guiaDeOutraOcs_recebe403() throws Exception {
        Guia daOutra = criarGuia(outraOcs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.ENVIADA);
        Sessao sessao = criarSessao(criarProcedimento(daOutra, "41001109", 1), 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(daOutra, sessao)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isForbidden());
    }

    @Test
    void guiaNaoEnviada_recebe404() throws Exception {
        Guia naoEnviada = criarGuiaNaoEnviada(ocs);
        Sessao sessao = criarSessao(criarProcedimento(naoEnviada, "41001109", 1), 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(naoEnviada, sessao)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isNotFound());
    }

    @Test
    void sessaoDeOutraGuia_recebe404() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.ENVIADA);
        Guia outra = criarGuia(ocs, LocalDateTime.of(2026, 10, 2, 10, 0), FUTURA, SituacaoExecucao.ENVIADA);
        Sessao daOutra = criarSessao(criarProcedimento(outra, "41001109", 1), 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(guia, daOutra)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isNotFound());
    }

    @Test
    void guiaInexistente_recebe404() throws Exception {
        mockMvc.perform(post("/ocs/guias/999999/sessoes/1/solicitacao")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isNotFound());
    }

    @Test
    void guiaEncerrada_recebe409() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA, SituacaoExecucao.EM_EXECUCAO);
        GuiaProcedimento exame = criarProcedimento(guia, "41001109", 2);
        criarSessao(exame, 1, SituacaoSessao.ASSINADA);
        Sessao disponivel = criarSessao(exame, 2, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(guia, disponivel)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isConflict());
    }

    @Test
    void guiaVencidaSemAssinatura_recebe409() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA, SituacaoExecucao.ENVIADA);
        Sessao disponivel = criarSessao(criarProcedimento(guia, "41001109", 1), 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(guia, disponivel)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isConflict());
    }

    @Test
    void perfilDiferenteDeOcs_recebe403() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA, SituacaoExecucao.ENVIADA);
        Sessao sessao = criarSessao(criarProcedimento(guia, "41001109", 1), 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post(url(guia, sessao)).header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
    }

    // ---------- fixtures ----------

    private static String url(Guia guia, Sessao sessao) {
        return "/ocs/guias/" + guia.getIdGuia() + "/sessoes/" + sessao.getIdSessao() + "/solicitacao";
    }

    private Ocs novaOcs(String nome, String contrato) {
        Ocs o = new Ocs();
        o.setNome(nome);
        o.setContratoNum(contrato);
        o.setTipo(OcsTipo.OCS);
        return o;
    }

    private Usuario vincularConta(String login, Ocs alvo) {
        Usuario conta = usuario(login, PerfilUsuario.OCS);
        ContaOcs contaOcs = new ContaOcs();
        contaOcs.setUsuario(conta);
        contaOcs.setOcs(alvo);
        contaOcsRepository.save(contaOcs);
        return conta;
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

    private Guia criarGuia(Ocs ocsDaGuia, LocalDateTime envio, LocalDate validade, SituacaoExecucao situacao) {
        Guia guia = novaGuia(ocsDaGuia, validade, situacao);
        guia.setDataEnvio(envio);
        guia.setEnviadaPor(usuarioJoao);
        return guiaRepository.save(guia);
    }

    private Guia criarGuiaNaoEnviada(Ocs ocsDaGuia) {
        return guiaRepository.save(novaGuia(ocsDaGuia, FUTURA, SituacaoExecucao.NAO_ENVIADA));
    }

    private Guia novaGuia(Ocs ocsDaGuia, LocalDate validade, SituacaoExecucao situacao) {
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
        return guia;
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
