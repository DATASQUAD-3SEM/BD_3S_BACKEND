package fatec.fusex.nexus.setorFatura;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoRepository;
import fatec.fusex.nexus.espelho.OrigemContrato;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.espelho.StatusEspelho;
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
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FaturaAuditadaControllerTest {

    private static final String LOGIN_SETOR = "setor.auditadas";
    private static final String LOGIN_OCS = "ocs.auditadas";
    private static final String LOGIN_BENEFICIARIO = "33344455566";
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String URL = "/setor-fatura/faturas-auditadas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OcsRepository ocsRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private PreGuiaRepository preGuiaRepository;

    @Autowired
    private GuiaRepository guiaRepository;

    @Autowired
    private GuiaProcedimentoRepository procedimentoRepository;

    @Autowired
    private EspelhoRepository espelhoRepository;

    @Autowired
    private EspelhoItemRepository espelhoItemRepository;

    @Autowired
    private FaturaEventoRepository eventoRepository;

    private Ocs ocs;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    @BeforeEach
    void cenario() {
        Ocs nova = new Ocs();
        nova.setNome("Laboratorio Auditadas");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-AUDITADAS");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_OCS, PerfilUsuario.OCS);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Joao Auditadas");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void soTrazFaturasAuditadasEValorFinalIgualInformadoMenosGlosa() throws Exception {
        Espelho auditada = novoEspelho(SituacaoFatura.AUDITADA, "001/2026",
                LocalDateTime.of(2026, 11, 5, 9, 0), LocalDateTime.of(2026, 11, 15, 9, 0));
        novoItem(auditada, "41001101", new BigDecimal("100.00"), new BigDecimal("80.00"));

        novoEspelho(SituacaoFatura.RECEBIDO, null, LocalDateTime.of(2026, 11, 1, 9, 0), null);
        novoEspelho(SituacaoFatura.EM_LISURA, "002/2026", LocalDateTime.of(2026, 11, 2, 9, 0),
                LocalDateTime.of(2026, 11, 3, 9, 0));

        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(auditada.getIdEspelho()))
                .andExpect(jsonPath("$[0].numeroFatura").value("001/2026"))
                .andExpect(jsonPath("$[0].valorInformado", closeTo(200.0, 0.001)))
                .andExpect(jsonPath("$[0].valorGlosa", closeTo(40.0, 0.001)))
                .andExpect(jsonPath("$[0].valorFinal", closeTo(160.0, 0.001)));
    }

    @Test
    void ordemPelaDataDaVisualizacaoMaisRecentePrimeiro() throws Exception {
        novoEspelho(SituacaoFatura.AUDITADA, "001/2026",
                LocalDateTime.of(2026, 11, 5, 9, 0), LocalDateTime.of(2026, 11, 10, 9, 0));
        novoEspelho(SituacaoFatura.AUDITADA, "002/2026",
                LocalDateTime.of(2026, 11, 15, 9, 0), LocalDateTime.of(2026, 11, 15, 9, 0));

        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numeroFatura").value("002/2026"))
                .andExpect(jsonPath("$[1].numeroFatura").value("001/2026"));
    }

    @Test
    void glosaComSinalNegativoQuandoValorFinalMaiorQueInformado() throws Exception {
        Espelho auditada = novoEspelho(SituacaoFatura.AUDITADA, "001/2026",
                LocalDateTime.of(2026, 11, 5, 9, 0), LocalDateTime.of(2026, 11, 15, 9, 0));
        novoItem(auditada, "41001101", new BigDecimal("100.00"), new BigDecimal("120.00"));

        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valorInformado", closeTo(200.0, 0.001)))
                .andExpect(jsonPath("$[0].valorGlosa", closeTo(-40.0, 0.001)))
                .andExpect(jsonPath("$[0].valorFinal", closeTo(240.0, 0.001)));
    }

    @Test
    void listaVaziaQuandoNaoHaAuditadas() throws Exception {
        novoEspelho(SituacaoFatura.RECEBIDO, null, LocalDateTime.of(2026, 11, 1, 9, 0), null);

        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void perfilSemPermissaoRecebe403() throws Exception {
        novoEspelho(SituacaoFatura.AUDITADA, "001/2026",
                LocalDateTime.of(2026, 11, 5, 9, 0), LocalDateTime.of(2026, 11, 15, 9, 0));

        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isForbidden());
    }

    private Espelho novoEspelho(SituacaoFatura situacao, String numero, LocalDateTime envio,
                                LocalDateTime visualizacao) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(envio);
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura(numero);
        espelho.setProtocoladaPor(numero == null ? null : setor);
        espelho = espelhoRepository.save(espelho);

        if (visualizacao != null) {
            FaturaEvento evento = new FaturaEvento();
            evento.setEspelho(espelho);
            evento.setTipo(TipoFaturaEvento.VISUALIZADA);
            evento.setUsuario(setor);
            evento.setResponsavel("Ana Silva");
            evento.setDataEvento(visualizacao);
            eventoRepository.save(evento);
        }
        return espelho;
    }

    private EspelhoItem novoItem(Espelho espelho, String codDgp, BigDecimal valorSessao, BigDecimal contrato) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(), codDgp));
        item.setQuantidadeSessoes(2);
        item.setValorSessao(valorSessao);
        item.setValorContratoSessao(contrato);
        item.setOrigemContrato(contrato == null ? null : OrigemContrato.CONFERIDO);
        return espelhoItemRepository.save(item);
    }

    private Guia novaGuia() {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setBeneficiario(joao);
        preGuia.setOcs(ocs);
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

    private GuiaProcedimento novoProcedimento(Guia guia, String codDgp) {
        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guia);
        procedimento.setCodDgp(codDgp);
        procedimento.setDescricaoProcedimento("TC - Abdome superior");
        procedimento.setQuantidadeAutorizada(2);
        return procedimentoRepository.save(procedimento);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }
}