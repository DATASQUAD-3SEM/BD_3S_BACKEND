package fatec.fusex.nexus.guiaFinalizada;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.StatusEspelho;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaFinalizadaControllerTest {

    private static final LocalDate PASSADA = LocalDate.of(2020, 1, 1);
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String LOGIN_OCS = "ocs.lab";
    private static final String LOGIN_OUTRA_OCS = "ocs.outra";
    private static final String LOGIN_BENEFICIARIO = "11111111111";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final ContaOcsRepository contaOcsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final EspelhoRepository espelhoRepository;

    private Ocs ocs;
    private Ocs outraOcs;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    GuiaFinalizadaControllerTest(MockMvc mockMvc,
                                 UsuarioRepository usuarioRepository,
                                 BeneficiarioRepository beneficiarioRepository,
                                 OcsRepository ocsRepository,
                                 ContaOcsRepository contaOcsRepository,
                                 PreGuiaRepository preGuiaRepository,
                                 GuiaRepository guiaRepository,
                                 GuiaProcedimentoRepository procedimentoRepository,
                                 SessaoRepository sessaoRepository,
                                 EspelhoRepository espelhoRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.contaOcsRepository = contaOcsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.espelhoRepository = espelhoRepository;
    }

    @BeforeEach
    void setUp() {
        ocs = novaOcs("Laboratorio Lab", "CTR-LAB");
        outraOcs = novaOcs("Clinica Outra", "CTR-OUT");

        vincularConta(novoUsuario(LOGIN_OCS, PerfilUsuario.OCS), ocs);
        vincularConta(novoUsuario(LOGIN_OUTRA_OCS, PerfilUsuario.OCS), outraOcs);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        joao = new Beneficiario();
        joao.setUsuario(usuarioJoao);
        joao.setNome("Joao Logado");
        joao.setPrecCp(LOGIN_BENEFICIARIO);
        joao.setDataNascimento(LocalDate.of(1980, 1, 1));
        joao.setTelefone("11999999999");
        joao = beneficiarioRepository.save(joao);

        setor = novoUsuario("setor01", PerfilUsuario.SETOR_GUIAS);
    }

    @Test
    void listaSoRealizadasEEncerradasDaOcsLogada_damaisRecenteParaAMaisAntiga() throws Exception {
        Guia realizada = novaGuia(ocs, SituacaoExecucao.REALIZADA, FUTURA, LocalDateTime.of(2026, 10, 1, 10, 0));
        Guia encerrada = novaGuia(ocs, SituacaoExecucao.ENCERRADA, FUTURA, LocalDateTime.of(2026, 10, 20, 10, 0));
        novaGuia(ocs, SituacaoExecucao.ENVIADA, FUTURA, LocalDateTime.of(2026, 10, 25, 10, 0));
        novaGuia(ocs, SituacaoExecucao.EM_EXECUCAO, FUTURA, LocalDateTime.of(2026, 10, 26, 10, 0));
        novaGuia(ocs, SituacaoExecucao.VENCIDA, FUTURA, LocalDateTime.of(2026, 10, 27, 10, 0));
        novaGuia(ocs, SituacaoExecucao.NAO_ENVIADA, FUTURA, null);
        novaGuia(outraOcs, SituacaoExecucao.REALIZADA, FUTURA, LocalDateTime.of(2026, 10, 28, 10, 0));

        mockMvc.perform(get("/ocs/guias-finalizadas").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(encerrada.getIdGuia()))
                .andExpect(jsonPath("$[0].situacao").value("ENCERRADA"))
                .andExpect(jsonPath("$[1].id").value(realizada.getIdGuia()))
                .andExpect(jsonPath("$[1].situacao").value("REALIZADA"));
    }

    @Test
    void vinculoDaGuia_disponivelEmConstrucaoOuEnviada() throws Exception {
        Espelho emConstrucao = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        Espelho enviado = novoEspelho(ocs, StatusEspelho.ENVIADO);

        Guia disponivel = novaGuia(ocs, SituacaoExecucao.REALIZADA, FUTURA, LocalDateTime.of(2026, 10, 1, 10, 0));
        Guia noConstrucao = novaGuia(ocs, SituacaoExecucao.REALIZADA, FUTURA, LocalDateTime.of(2026, 10, 2, 10, 0));
        noConstrucao.setEspelho(emConstrucao);
        guiaRepository.save(noConstrucao);
        Guia jaEnviada = novaGuia(ocs, SituacaoExecucao.REALIZADA, FUTURA, LocalDateTime.of(2026, 10, 3, 10, 0));
        jaEnviada.setEspelho(enviado);
        guiaRepository.save(jaEnviada);

        mockMvc.perform(get("/ocs/guias-finalizadas").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(jaEnviada.getIdGuia()))
                .andExpect(jsonPath("$[0].vinculo").value("ENVIADA"))
                .andExpect(jsonPath("$[0].idEspelho").value(enviado.getIdEspelho()))
                .andExpect(jsonPath("$[1].id").value(noConstrucao.getIdGuia()))
                .andExpect(jsonPath("$[1].vinculo").value("EM_CONSTRUCAO"))
                .andExpect(jsonPath("$[1].idEspelho").value(emConstrucao.getIdEspelho()))
                .andExpect(jsonPath("$[2].id").value(disponivel.getIdGuia()))
                .andExpect(jsonPath("$[2].vinculo").value("DISPONIVEL"))
                .andExpect(jsonPath("$[2].idEspelho").doesNotExist());
    }

    @Test
    void viradaAntesDaLista_guiaEmExecucaoVencidaComSessaoAssinada_apareceComoEncerrada() throws Exception {
        Guia guia = novaGuia(ocs, SituacaoExecucao.EM_EXECUCAO, PASSADA, LocalDateTime.of(2026, 10, 1, 10, 0));
        GuiaProcedimento exame = novoProcedimento(guia, "41001109", 2);
        novaSessao(exame, 1, SituacaoSessao.ASSINADA);
        novaSessao(exame, 2, SituacaoSessao.SOLICITADA);

        mockMvc.perform(get("/ocs/guias-finalizadas").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(guia.getIdGuia()))
                .andExpect(jsonPath("$[0].situacao").value("ENCERRADA"))
                .andExpect(jsonPath("$[0].procedimentos", hasSize(1)))
                .andExpect(jsonPath("$[0].procedimentos[0].codDgp").value("41001109"))
                .andExpect(jsonPath("$[0].procedimentos[0].quantidadeAssinadas").value(1));

        assertSituacao(guia, SituacaoExecucao.ENCERRADA);
    }

    @Test
    void viradaAntesDaLista_guiaEmExecucaoVencidaSemAssinatura_naoAparece() throws Exception {
        Guia guia = novaGuia(ocs, SituacaoExecucao.EM_EXECUCAO, PASSADA, LocalDateTime.of(2026, 10, 1, 10, 0));
        GuiaProcedimento exame = novoProcedimento(guia, "41001109", 1);
        novaSessao(exame, 1, SituacaoSessao.SOLICITADA);

        mockMvc.perform(get("/ocs/guias-finalizadas").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertSituacao(guia, SituacaoExecucao.VENCIDA);
    }

    @Test
    void semGuiasFinalizadas_devolveListaVazia() throws Exception {
        novaGuia(ocs, SituacaoExecucao.ENVIADA, FUTURA, LocalDateTime.of(2026, 10, 1, 10, 0));

        mockMvc.perform(get("/ocs/guias-finalizadas").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void usuarioSemPerfilOcs_recebe403() throws Exception {
        mockMvc.perform(get("/ocs/guias-finalizadas").header(SimulacaoLoginFilter.HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isForbidden());
    }

    private Ocs novaOcs(String nome, String contrato) {
        Ocs o = new Ocs();
        o.setNome(nome);
        o.setTipo(OcsTipo.OCS);
        o.setContratoNum(contrato);
        return ocsRepository.save(o);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(perfil);
        return usuarioRepository.save(u);
    }

    private void vincularConta(Usuario usuario, Ocs ocsDaConta) {
        ContaOcs conta = new ContaOcs();
        conta.setUsuario(usuario);
        conta.setOcs(ocsDaConta);
        contaOcsRepository.save(conta);
    }

    private Guia novaGuia(Ocs ocsDaGuia, SituacaoExecucao situacao, LocalDate validade, LocalDateTime envio) {
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
            guia.setEnviadaPor(setor);
        }
        return guiaRepository.save(guia);
    }

    private GuiaProcedimento novoProcedimento(Guia guia, String codDgp, int quantidade) {
        GuiaProcedimento p = new GuiaProcedimento();
        p.setGuia(guia);
        p.setCodDgp(codDgp);
        p.setDescricaoProcedimento("TC - Abdome superior");
        p.setQuantidadeAutorizada(quantidade);
        return procedimentoRepository.save(p);
    }

    private Sessao novaSessao(GuiaProcedimento procedimento, int numero, SituacaoSessao status) {
        Sessao s = new Sessao();
        s.setGuiaProcedimento(procedimento);
        s.setNumero(numero);
        s.setStatus(status);
        if (status != SituacaoSessao.DISPONIVEL) {
            s.setDataSolicitacao(LocalDateTime.of(2026, 10, 2, 9, 0));
            s.setSolicitadaPor(setor);
        }
        if (status == SituacaoSessao.ASSINADA) {
            s.setDataAssinatura(LocalDateTime.of(2026, 10, 2, 10, 0));
            s.setAssinadoPor(setor);
        }
        return sessaoRepository.save(s);
    }

    private Espelho novoEspelho(Ocs ocsDoEspelho, StatusEspelho status) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocsDoEspelho);
        espelho.setStatus(status);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 10, 1, 8, 0));
        if (status == StatusEspelho.ENVIADO) {
            espelho.setDataEnvio(LocalDateTime.of(2026, 10, 31, 18, 0));
            espelho.setEnviadoPor(setor);
        }
        return espelhoRepository.save(espelho);
    }

    private void assertSituacao(Guia guia, SituacaoExecucao esperada) {
        guiaRepository.flush();
        Guia atual = guiaRepository.findById(guia.getIdGuia()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(esperada, atual.getSituacaoExecucao());
    }
}