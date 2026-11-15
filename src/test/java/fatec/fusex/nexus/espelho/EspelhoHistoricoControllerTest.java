package fatec.fusex.nexus.espelho;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class EspelhoHistoricoControllerTest {

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
    private final EspelhoRepository espelhoRepository;
    private final EspelhoItemRepository espelhoItemRepository;

    private Ocs ocs;
    private Ocs outraOcs;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    EspelhoHistoricoControllerTest(MockMvc mockMvc,
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
    void listaSoEspelhosEnviadosDaOcsLogada_maisRecentePrimeiro() throws Exception {
        Espelho antigo = novoEspelhoEnviado(ocs, LocalDateTime.of(2026, 9, 30, 18, 0));
        Espelho recente = novoEspelhoEnviado(ocs, LocalDateTime.of(2026, 10, 31, 18, 0));
        vincular(antigo, novaGuiaComDoisExames(ocs), new BigDecimal("100.00"));
        vincular(recente, novaGuiaComDoisExames(ocs), new BigDecimal("200.00"));

        Espelho emConstrucao = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        vincular(emConstrucao, novaGuiaComDoisExames(ocs), new BigDecimal("50.00"));

        Espelho deOutraOcs = novoEspelhoEnviado(outraOcs, LocalDateTime.of(2026, 10, 31, 19, 0));
        vincular(deOutraOcs, novaGuiaComDoisExames(outraOcs), new BigDecimal("80.00"));

        mockMvc.perform(get("/ocs/espelho/historico").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(recente.getIdEspelho()))
                .andExpect(jsonPath("$[0].valorTotal", closeTo(600.0, 0.001)))
                .andExpect(jsonPath("$[0].itens", hasSize(2)))
                .andExpect(jsonPath("$[1].id").value(antigo.getIdEspelho()))
                .andExpect(jsonPath("$[1].valorTotal", closeTo(300.0, 0.001)));
    }

    @Test
    void semEspelhosEnviados_devolveListaVazia() throws Exception {
        novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);

        mockMvc.perform(get("/ocs/espelho/historico").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void usuarioSemPerfilOcs_recebe403() throws Exception {
        mockMvc.perform(get("/ocs/espelho/historico").header(SimulacaoLoginFilter.HEADER, LOGIN_BENEFICIARIO))
                .andExpect(status().isForbidden());
    }

    private Guia novaGuiaComDoisExames(Ocs ocsDaGuia) {
        Guia guia = novaGuia(ocsDaGuia);
        novoProcedimento(guia, "41001109", 2);
        novoProcedimento(guia, "41001110", 1);
        return guia;
    }

    private void vincular(Espelho espelho, Guia guia, BigDecimal valorSessao) {
        List<GuiaProcedimento> procedimentos = procedimentoRepository
                .findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(List.of(guia.getIdGuia()));
        for (GuiaProcedimento procedimento : procedimentos) {
            EspelhoItem item = new EspelhoItem();
            item.setEspelho(espelho);
            item.setGuiaProcedimento(procedimento);
            item.setQuantidadeSessoes(procedimento.getQuantidadeAutorizada());
            item.setValorSessao(valorSessao);
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
        return espelhoRepository.save(espelho);
    }

    private Espelho novoEspelhoEnviado(Ocs ocsDoEspelho, LocalDateTime dataEnvio) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocsDoEspelho);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 9, 1, 8, 0));
        espelho.setDataEnvio(dataEnvio);
        espelho.setEnviadoPor(setor);
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