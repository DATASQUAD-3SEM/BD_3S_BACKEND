package fatec.fusex.nexus.rastreabilidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaEventoRepository;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LisuraEnvioOcsControllerTest {

    private static final String LOGIN_RASTREABILIDADE = "rastreabilidade.envio";
    private static final String LOGIN_SETOR = "setor.envio.lisura";
    private static final String LOGIN_BENEFICIARIO = "55566677788";
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String URL_ENVIO = "/rastreabilidade/faturas/{id}/enviar-ocs";
    private static final String URL_LISURA = "/rastreabilidade/faturas/{id}/lisura";

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
        nova.setNome("Laboratorio Envio OCS");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-ENVIO-OCS");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_RASTREABILIDADE, PerfilUsuario.RASTREABILIDADE);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Joao Envio");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void comGlosaETodasDecididasVaiParaAguardandoOcsComEvento() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL);

        mockMvc.perform(post(URL_ENVIO, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavel\":\"Ana Silva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_OCS"))
                .andExpect(jsonPath("$.eventos", hasSize(1)))
                .andExpect(jsonPath("$.eventos[0].tipo").value("ENVIADA_OCS"))
                .andExpect(jsonPath("$.eventos[0].responsavel").value("Ana Silva"))
                .andExpect(jsonPath("$.eventos[0].usuario").value(LOGIN_RASTREABILIDADE));
    }

    @Test
    void semResponsavelRecebe422ENadaMuda() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL);

        mockMvc.perform(post(URL_ENVIO, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavel\":\"  \"}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(espelhoRepository.findById(espelho.getIdEspelho()).orElseThrow().getSituacaoFatura())
                .isEqualTo(SituacaoFatura.EM_LISURA);
    }

    @Test
    void linhaSemDecisaoRecebe422ENadaMuda() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL);
        novoItem(espelho, "41001102", new BigDecimal("100.00"), 1, null, null);

        mockMvc.perform(post(URL_ENVIO, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavel\":\"Ana Silva\"}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(espelhoRepository.findById(espelho.getIdEspelho()).orElseThrow().getSituacaoFatura())
                .isEqualTo(SituacaoFatura.EM_LISURA);
    }

    @Test
    void faturaSemGlosaRecebe409() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("100.00"),
                OrigemContrato.CONFERIDO);

        mockMvc.perform(post(URL_ENVIO, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavel\":\"Ana Silva\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Esta fatura não tem glosa; envie direto para a auditoria."));
    }

    @Test
    void salvarALisuraDepoisDoEnvioRecebe409() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, "001/2026");
        EspelhoItem item = novoItem(espelho, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL);

        mockMvc.perform(post(URL_ENVIO, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavel\":\"Ana Silva\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(URL_LISURA, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem() + ",\"conforme\":true}]"))
                .andExpect(status().isConflict());
    }

    @Test
    void faturaForaDeLisuraRecebe409() throws Exception {
        Espelho protocolado = novoEspelho(SituacaoFatura.PROTOCOLADA, "001/2026");
        novoItem(protocolado, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL);

        mockMvc.perform(post(URL_ENVIO, protocolado.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavel\":\"Ana Silva\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reenvioDepoisDeContestacaoAcrescentaEventoSemApagarOAnterior() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, "001/2026");
        registrarEvento(espelho, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.of(2000, 1, 3, 9, 0), "Ana Silva");
        registrarEvento(espelho, TipoFaturaEvento.CONTESTADA_OCS, LocalDateTime.of(2000, 1, 5, 9, 0), "Bruno Lima");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL);

        mockMvc.perform(post(URL_ENVIO, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavel\":\"Carla Souza\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_OCS"))
                .andExpect(jsonPath("$.eventos", hasSize(3)))
                .andExpect(jsonPath("$.eventos[0].tipo").value("ENVIADA_OCS"))
                .andExpect(jsonPath("$.eventos[1].tipo").value("CONTESTADA_OCS"))
                .andExpect(jsonPath("$.eventos[2].tipo").value("ENVIADA_OCS"))
                .andExpect(jsonPath("$.eventos[2].responsavel").value("Carla Souza"));
    }

    private Espelho novoEspelho(SituacaoFatura situacao, String numero) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(LocalDateTime.of(2026, 11, 10, 9, 0));
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura(numero);
        espelho.setProtocoladaPor(setor);
        return espelhoRepository.save(espelho);
    }

    private void registrarEvento(Espelho espelho, TipoFaturaEvento tipo, LocalDateTime data, String responsavel) {
        FaturaEvento evento = new FaturaEvento();
        evento.setEspelho(espelho);
        evento.setTipo(tipo);
        evento.setUsuario(setor);
        evento.setResponsavel(responsavel);
        evento.setDataEvento(data);
        eventoRepository.save(evento);
    }

    private EspelhoItem novoItem(Espelho espelho, String codDgp, BigDecimal valorSessao, int quantidade,
                                 BigDecimal contrato, OrigemContrato origem) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(), codDgp));
        item.setQuantidadeSessoes(quantidade);
        item.setValorSessao(valorSessao);
        item.setValorContratoSessao(contrato);
        item.setOrigemContrato(origem);
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