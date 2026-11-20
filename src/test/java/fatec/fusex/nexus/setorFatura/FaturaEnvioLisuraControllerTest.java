package fatec.fusex.nexus.setorFatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
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
import fatec.fusex.nexus.ocsPreco.OcsPreco;
import fatec.fusex.nexus.ocsPreco.OcsPrecoRepository;
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
class FaturaEnvioLisuraControllerTest {

    private static final String LOGIN_SETOR = "setor.envio.lisura";
    private static final String LOGIN_OCS = "ocs.envio.lisura";
    private static final String LOGIN_BENEFICIARIO = "44455566677";
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final LocalDateTime ENVIO = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final String RESPONSAVEL = "Ana Silva";
    private static final String URL = "/setor-fatura/faturas/{id}/enviar-lisura";

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
    private OcsPrecoRepository ocsPrecoRepository;

    private Ocs ocs;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    @BeforeEach
    void cenario() {
        Ocs nova = new Ocs();
        nova.setNome("Laboratorio Envio Lisura");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-ENVIO-LISURA");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_OCS, PerfilUsuario.OCS);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Joao Envio Lisura");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void faturaProtocoladaVaiParaEmLisuraComEventoEResponsavel() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.PROTOCOLADA, "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), null);

        mockMvc.perform(post(URL, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(RESPONSAVEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("EM_LISURA"))
                .andExpect(jsonPath("$.eventos", hasSize(1)))
                .andExpect(jsonPath("$.eventos[0].tipo").value("ENVIADA_LISURA"))
                .andExpect(jsonPath("$.eventos[0].responsavel").value(RESPONSAVEL))
                .andExpect(jsonPath("$.eventos[0].usuario").value(LOGIN_SETOR));
    }

    @Test
    void itemComPrecoNaTabelaCopiaValorEOrigemTabela() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.PROTOCOLADA, "001/2026");
        EspelhoItem item = novoItem(espelho, "41001101", new BigDecimal("100.00"), null);
        salvarPreco(ocs, "41001101", new BigDecimal("80.00"));

        mockMvc.perform(post(URL, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(RESPONSAVEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].valorContrato").value(80.0))
                .andExpect(jsonPath("$.linhas[0].origemContrato").value("TABELA"));

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.TABELA);
    }

    @Test
    void itemSemPrecoNaTabelaFicaComContratoEOrigemNulos() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.PROTOCOLADA, "001/2026");
        EspelhoItem item = novoItem(espelho, "41001102", new BigDecimal("100.00"), null);

        mockMvc.perform(post(URL, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(RESPONSAVEL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].valorContrato").value(nullValue()))
                .andExpect(jsonPath("$.linhas[0].origemContrato").value(nullValue()));

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getValorContratoSessao()).isNull();
        assertThat(gravado.getOrigemContrato()).isNull();
    }

    @Test
    void mudarOcsPrecoDepoisDoEnvioNaoAlteraOItem() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.PROTOCOLADA, "001/2026");
        EspelhoItem item = novoItem(espelho, "41001101", new BigDecimal("100.00"), null);
        salvarPreco(ocs, "41001101", new BigDecimal("80.00"));

        mockMvc.perform(post(URL, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(RESPONSAVEL)))
                .andExpect(status().isOk());

        OcsPreco preco = ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), "41001101").orElseThrow();
        preco.setValorSessao(new BigDecimal("99.00"));
        ocsPrecoRepository.saveAndFlush(preco);

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.TABELA);
    }

    @Test
    void semResponsavelRecebe422ENadaMuda() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.PROTOCOLADA, "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), null);

        mockMvc.perform(post(URL, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("  ")))
                .andExpect(status().isUnprocessableEntity());

        assertThat(espelhoRepository.findById(espelho.getIdEspelho()).orElseThrow().getSituacaoFatura())
                .isEqualTo(SituacaoFatura.PROTOCOLADA);
    }

    @Test
    void faturaForaDeProtocoladaRecebe409ENadaMuda() throws Exception {
        Espelho recebido = novoEspelho(SituacaoFatura.RECEBIDO, null);
        novoItem(recebido, "41001101", new BigDecimal("100.00"), null);

        mockMvc.perform(post(URL, recebido.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(RESPONSAVEL)))
                .andExpect(status().isConflict());

        assertThat(espelhoRepository.findById(recebido.getIdEspelho()).orElseThrow().getSituacaoFatura())
                .isEqualTo(SituacaoFatura.RECEBIDO);
    }

    @Test
    void perfilSemPermissaoRecebe403() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.PROTOCOLADA, "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), null);

        mockMvc.perform(post(URL, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(RESPONSAVEL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void faturaInexistenteRecebe404() throws Exception {
        mockMvc.perform(post(URL, 999999L)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(RESPONSAVEL)))
                .andExpect(status().isNotFound());
    }

    private String corpo(String responsavel) {
        return "{\"responsavel\":\"" + responsavel + "\"}";
    }

    private Espelho novoEspelho(SituacaoFatura situacao, String numero) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(ENVIO);
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura(numero);
        espelho.setProtocoladaPor(numero == null ? null : setor);
        return espelhoRepository.save(espelho);
    }

    private EspelhoItem novoItem(Espelho espelho, String codDgp, BigDecimal valorSessao, BigDecimal contrato) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(), codDgp));
        item.setQuantidadeSessoes(1);
        item.setValorSessao(valorSessao);
        item.setValorContratoSessao(contrato);
        item.setOrigemContrato(contrato == null ? null : OrigemContrato.TABELA);
        return espelhoItemRepository.save(item);
    }

    private void salvarPreco(Ocs dona, String codDgp, BigDecimal valor) {
        OcsPreco preco = new OcsPreco();
        preco.setOcs(dona);
        preco.setCodDgp(codDgp);
        preco.setValorSessao(valor);
        ocsPrecoRepository.save(preco);
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