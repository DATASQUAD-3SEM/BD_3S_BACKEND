package fatec.fusex.nexus.espelho;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class EspelhoEdicaoControllerTest {

    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String LOGIN_OCS = "ocs.lab";
    private static final String LOGIN_OUTRA_OCS = "ocs.outra";

    private static final String VALORES_NOVOS =
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
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;

    private Ocs ocs;
    private Ocs outraOcs;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    EspelhoEdicaoControllerTest(MockMvc mockMvc,
                                UsuarioRepository usuarioRepository,
                                BeneficiarioRepository beneficiarioRepository,
                                OcsRepository ocsRepository,
                                ContaOcsRepository contaOcsRepository,
                                PreGuiaRepository preGuiaRepository,
                                GuiaRepository guiaRepository,
                                GuiaProcedimentoRepository procedimentoRepository,
                                EspelhoRepository espelhoRepository,
                                EspelhoItemRepository espelhoItemRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.contaOcsRepository = contaOcsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.espelhoRepository = espelhoRepository;
        this.espelhoItemRepository = espelhoItemRepository;
    }

    @BeforeEach
    void setUp() {
        ocs = novaOcs("Laboratorio Lab", "CTR-LAB");
        outraOcs = novaOcs("Clinica Outra", "CTR-OUT");

        vincularConta(novoUsuario(LOGIN_OCS, PerfilUsuario.OCS), ocs);
        vincularConta(novoUsuario(LOGIN_OUTRA_OCS, PerfilUsuario.OCS), outraOcs);

        Usuario usuarioJoao = novoUsuario("11111111111", PerfilUsuario.BENEFICIARIO);
        joao = new Beneficiario();
        joao.setUsuario(usuarioJoao);
        joao.setNome("Joao Logado");
        joao.setPrecCp("11111111111");
        joao.setDataNascimento(LocalDate.of(1980, 1, 1));
        joao.setTelefone("11999999999");
        joao = beneficiarioRepository.save(joao);

        setor = novoUsuario("setor01", PerfilUsuario.SETOR_GUIAS);
    }

    @Test
    void editaValor_recalculaLinhaETotal_semAlterarQuantidade() throws Exception {
        Espelho espelho = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        Guia guia1 = novaGuiaComDoisExames(ocs);
        Guia guia2 = novaGuiaComDoisExames(ocs);
        vincular(espelho, guia1);
        vincular(espelho, guia2);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", guia1.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_NOVOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(4)))
                .andExpect(jsonPath("$.itens[0].guiaId").value(guia1.getIdGuia()))
                .andExpect(jsonPath("$.itens[0].quantidadeSessoes").value(2))
                .andExpect(jsonPath("$.itens[0].valorSessao", closeTo(150.0, 0.001)))
                .andExpect(jsonPath("$.itens[0].valorTotal", closeTo(300.0, 0.001)))
                .andExpect(jsonPath("$.itens[1].valorTotal", closeTo(240.5, 0.001)))
                .andExpect(jsonPath("$.itens[2].valorSessao", closeTo(100.0, 0.001)))
                .andExpect(jsonPath("$.valorTotal", closeTo(840.5, 0.001)));

        Guia atual = guiaRepository.findById(guia1.getIdGuia()).orElseThrow();
        Assertions.assertEquals(guia1.getNumero(), atual.getNumero());
        Assertions.assertEquals(FUTURA, atual.getDataValidadeAte());
        Assertions.assertEquals(SituacaoExecucao.REALIZADA, atual.getSituacaoExecucao());
    }

    @Test
    void guiaQueNaoEstaNoEspelho_recebe404() throws Exception {
        Espelho espelho = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        vincular(espelho, novaGuiaComDoisExames(ocs));
        Guia naoVinculada = novaGuiaComDoisExames(ocs);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", naoVinculada.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_NOVOS))
                .andExpect(status().isNotFound());
    }

    @Test
    void semEspelhoEmConstrucao_recebe409() throws Exception {
        Guia guia = novaGuiaComDoisExames(ocs);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_NOVOS))
                .andExpect(status().isConflict());
    }

    @Test
    void espelhoJaEnviado_recebe409() throws Exception {
        Espelho enviado = novoEspelho(ocs, StatusEspelho.ENVIADO);
        Guia guia = novaGuiaComDoisExames(ocs);
        vincular(enviado, guia);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_NOVOS))
                .andExpect(status().isConflict());
    }

    @Test
    void guiaDeOutraOcs_recebe403() throws Exception {
        Espelho espelho = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        vincular(espelho, novaGuiaComDoisExames(ocs));
        Guia guiaOutra = novaGuiaComDoisExames(outraOcs);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", guiaOutra.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_NOVOS))
                .andExpect(status().isForbidden());
    }

    @Test
    void valorZero_recebe400_enaoAlteraOItem() throws Exception {
        Espelho espelho = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        Guia guia = novaGuiaComDoisExames(ocs);
        vincular(espelho, guia);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"cod_dgp\":\"41001109\",\"valor_sessao\":0},"
                                + "{\"cod_dgp\":\"41001110\",\"valor_sessao\":240.50}]"))
                .andExpect(status().isBadRequest());

        BigDecimal valorAtual = espelhoItemRepository
                .buscarComDadosPorEspelho(espelho.getIdEspelho())
                .get(0)
                .getValorSessao();
        Assertions.assertEquals(0, new BigDecimal("100.00").compareTo(valorAtual));
    }

    @Test
    void procedimentoSemValor_recebe400() throws Exception {
        Espelho espelho = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        Guia guia = novaGuiaComDoisExames(ocs);
        vincular(espelho, guia);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"cod_dgp\":\"41001109\",\"valor_sessao\":150.00}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioSemPerfilOcs_recebe403() throws Exception {
        Espelho espelho = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        Guia guia = novaGuiaComDoisExames(ocs);
        vincular(espelho, guia);

        mockMvc.perform(put("/ocs/espelho/guias/{id}", guia.getIdGuia())
                        .header(SimulacaoLoginFilter.HEADER, "11111111111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALORES_NOVOS))
                .andExpect(status().isForbidden());
    }

    private Guia novaGuiaComDoisExames(Ocs ocsDaGuia) {
        Guia guia = novaGuia(ocsDaGuia);
        novoProcedimento(guia, "41001109", 2);
        novoProcedimento(guia, "41001110", 1);
        return guia;
    }

    private void vincular(Espelho espelho, Guia guia) {
        List<GuiaProcedimento> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(guia.getIdGuia()));
        for (GuiaProcedimento procedimento : procedimentos) {
            EspelhoItem item = new EspelhoItem();
            item.setEspelho(espelho);
            item.setGuiaProcedimento(procedimento);
            item.setQuantidadeSessoes(procedimento.getQuantidadeAutorizada());
            item.setValorSessao(new BigDecimal("100.00"));
            espelhoItemRepository.save(item);
        }
        guia.setEspelho(espelho);
        guiaRepository.save(guia);
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

    private Guia novaGuia(Ocs ocsDaGuia) {
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
        guia.setSituacaoExecucao(SituacaoExecucao.REALIZADA);
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
}