package fatec.fusex.nexus.espelho;

import static org.hamcrest.Matchers.closeTo;
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

import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class EspelhoControllerTest {

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

    EspelhoControllerTest(MockMvc mockMvc,
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
    void semEspelhoEmConstrucao_devolveEstruturaVazia() throws Exception {
        mockMvc.perform(get("/ocs/espelho").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.dataInicio").doesNotExist())
                .andExpect(jsonPath("$.itens", hasSize(0)))
                .andExpect(jsonPath("$.valorTotal", closeTo(0.0, 0.001)));
    }

    @Test
    void listaSoAsLinhasDoEspelhoEmConstrucaoDaOcsLogada_comValorTotal() throws Exception {
        Espelho espelho = novoEspelho(ocs, StatusEspelho.EM_CONSTRUCAO);
        Guia guiaA = novaGuia(ocs);
        GuiaProcedimento exameA = novoProcedimento(guiaA, "41001109", 2);
        GuiaProcedimento exameB = novoProcedimento(guiaA, "41001110", 1);
        novoItem(espelho, exameA, 2, "150.00");
        novoItem(espelho, exameB, 1, "240.50");

        Espelho espelhoOutra = novoEspelho(outraOcs, StatusEspelho.EM_CONSTRUCAO);
        Guia guiaOutra = novaGuia(outraOcs);
        novoItem(espelhoOutra, novoProcedimento(guiaOutra, "41001111", 1), 1, "99.00");

        mockMvc.perform(get("/ocs/espelho").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(espelho.getIdEspelho()))
                .andExpect(jsonPath("$.dataInicio").exists())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].guiaId").value(guiaA.getIdGuia()))
                .andExpect(jsonPath("$.itens[0].numeroGuia").value(guiaA.getNumero()))
                .andExpect(jsonPath("$.itens[0].paciente").value("Joao Logado"))
                .andExpect(jsonPath("$.itens[0].codDgp").value("41001109"))
                .andExpect(jsonPath("$.itens[0].quantidadeSessoes").value(2))
                .andExpect(jsonPath("$.itens[0].valorTotal", closeTo(300.0, 0.001)))
                .andExpect(jsonPath("$.itens[1].codDgp").value("41001110"))
                .andExpect(jsonPath("$.itens[1].valorTotal", closeTo(240.5, 0.001)))
                .andExpect(jsonPath("$.valorTotal", closeTo(540.5, 0.001)));
    }

    @Test
    void espelhoJaEnviado_naoAparecePeloEmConstrucao() throws Exception {
        novoEspelho(ocs, StatusEspelho.ENVIADO);

        mockMvc.perform(get("/ocs/espelho").header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    @Test
    void usuarioSemPerfilOcs_recebe403() throws Exception {
        mockMvc.perform(get("/ocs/espelho").header(SimulacaoLoginFilter.HEADER, LOGIN_BENEFICIARIO))
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
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
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

    private EspelhoItem novoItem(Espelho espelho, GuiaProcedimento procedimento, int quantidade, String valor) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(procedimento);
        item.setQuantidadeSessoes(quantidade);
        item.setValorSessao(new BigDecimal(valor));
        return espelhoItemRepository.save(item);
    }
}