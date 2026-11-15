package fatec.fusex.nexus.espelho;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class EspelhoGuiaControllerTest {

    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String LOGIN_OCS = "ocs.lab";
    private static final String LOGIN_OUTRA_OCS = "ocs.outra";
    private static final String LOGIN_BENEFICIARIO = "11111111111";

    private static final String VALORES_OK =
            "[{\"cod_dgp\":\"41001109\",\"valor_sessao\":150.00},"
                    + "{\"cod_dgp\":\"41001110\",\"valor_sessao\":240.50}]";

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

    EspelhoGuiaControllerTest(MockMvc mockMvc,
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
    void adicionaGuiaFinalizada_criaEspelhoComQuantidadeAssinadaEValorTotal() throws Exception {
        Guia guia = guiaComDoisExames(ocs, SituacaoExecucao.REALIZADA);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_OK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].codDgp").value("41001109"))
                .andExpect(jsonPath("$.itens[0].quantidadeSessoes").value(2))
                .andExpect(jsonPath("$.itens[0].valorTotal", closeTo(300.0, 0.001)))
                .andExpect(jsonPath("$.itens[1].quantidadeSessoes").value(1))
                .andExpect(jsonPath("$.itens[1].valorTotal", closeTo(240.5, 0.001)))
                .andExpect(jsonPath("$.valorTotal", closeTo(540.5, 0.001)));

        Guia atual = guiaRepository.findById(guia.getIdGuia()).orElseThrow();
        Assertions.assertNotNull(atual.getEspelho());
        Assertions.assertEquals(SituacaoExecucao.REALIZADA, atual.getSituacaoExecucao());
        Assertions.assertEquals(guia.getNumero(), atual.getNumero());
        Assertions.assertEquals(FUTURA, atual.getDataValidadeAte());
    }

    @Test
    void segundaGuia_entraNoMesmoEspelhoEmConstrucao() throws Exception {
        Guia primeira = guiaComDoisExames(ocs, SituacaoExecucao.REALIZADA);
        Guia segunda = guiaComUmExame(ocs, SituacaoExecucao.ENCERRADA, "41001111", 1);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", primeira.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_OK))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/ocs/espelho/guias/{id}", segunda.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"cod_dgp\":\"41001111\",\"valor_sessao\":99.00}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.valorTotal", closeTo(639.5, 0.001)));

        Assertions.assertTrue(espelhoRepository
                .findByOcsIdOcsAndStatus(ocs.getIdOcs(), StatusEspelho.EM_CONSTRUCAO)
                .isPresent());
    }

    @Test
    void guiaJaVinculadaAEspelho_recebe409() throws Exception {
        Guia guia = guiaComDoisExames(ocs, SituacaoExecucao.REALIZADA);
        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_OK))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_OK))
                .andExpect(status().isConflict());
    }

    @Test
    void guiaNaoFinalizada_recebe409() throws Exception {
        Guia guia = guiaComUmExame(ocs, SituacaoExecucao.ENVIADA, "41001109", 1);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"cod_dgp\":\"41001109\",\"valor_sessao\":150.00}]"))
                .andExpect(status().isConflict());
    }

    @Test
    void guiaDeOutraOcs_recebe403() throws Exception {
        Guia guia = guiaComDoisExames(outraOcs, SituacaoExecucao.REALIZADA);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_OK))
                .andExpect(status().isForbidden());
    }

    @Test
    void guiaInexistente_recebe404() throws Exception {
        mockMvc.perform(post("/ocs/espelho/guias/{id}", 999999L)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_OK))
                .andExpect(status().isNotFound());
    }

    @Test
    void valorZeroOuNegativo_recebe400() throws Exception {
        Guia guia = guiaComDoisExames(ocs, SituacaoExecucao.REALIZADA);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"cod_dgp\":\"41001109\",\"valor_sessao\":0},"
                                + "{\"cod_dgp\":\"41001110\",\"valor_sessao\":240.50}]"))
                .andExpect(status().isBadRequest());

        Assertions.assertNull(guiaRepository.findById(guia.getIdGuia()).orElseThrow().getEspelho());
    }

    @Test
    void procedimentoSemValor_recebe400() throws Exception {
        Guia guia = guiaComDoisExames(ocs, SituacaoExecucao.REALIZADA);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"cod_dgp\":\"41001109\",\"valor_sessao\":150.00}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void procedimentoSemSessaoAssinada_recebe400() throws Exception {
        Guia guia = novaGuia(ocs, SituacaoExecucao.REALIZADA);
        GuiaProcedimento exame = novoProcedimento(guia, "41001109", 1);
        novaSessao(exame, 1, SituacaoSessao.DISPONIVEL);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"cod_dgp\":\"41001109\",\"valor_sessao\":150.00}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioSemPerfilOcs_recebe403() throws Exception {
        Guia guia = guiaComDoisExames(ocs, SituacaoExecucao.REALIZADA);

        mockMvc.perform(post("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_BENEFICIARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_OK))
                .andExpect(status().isForbidden());
    }

    private Guia guiaComDoisExames(Ocs ocsDaGuia, SituacaoExecucao situacao) {
        Guia guia = novaGuia(ocsDaGuia, situacao);
        GuiaProcedimento exameA = novoProcedimento(guia, "41001109", 2);
        GuiaProcedimento exameB = novoProcedimento(guia, "41001110", 1);
        novaSessao(exameA, 1, SituacaoSessao.ASSINADA);
        novaSessao(exameA, 2, SituacaoSessao.ASSINADA);
        novaSessao(exameB, 1, SituacaoSessao.ASSINADA);
        return guia;
    }

    private Guia guiaComUmExame(Ocs ocsDaGuia, SituacaoExecucao situacao, String codDgp, int quantidade) {
        Guia guia = novaGuia(ocsDaGuia, situacao);
        GuiaProcedimento exame = novoProcedimento(guia, codDgp, quantidade);
        for (int numero = 1; numero <= quantidade; numero++) {
            novaSessao(exame, numero, SituacaoSessao.ASSINADA);
        }
        return guia;
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

    private Guia novaGuia(Ocs ocsDaGuia, SituacaoExecucao situacao) {
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
        guia.setDataValidadeAte(FUTURA);
        guia.setSituacaoExecucao(situacao);
        guia.setDataEnvio(LocalDateTime.of(2026, 10, 1, 10, 0));
        guia.setEnviadaPor(setor);
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
}