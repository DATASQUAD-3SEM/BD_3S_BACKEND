package fatec.fusex.nexus.rastreabilidade;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class LisuraConsultaControllerTest {

    private static final String LOGIN_RASTREABILIDADE = "rastreabilidade.consulta";
    private static final String LOGIN_SETOR = "setor.consulta.lisura";
    private static final String LOGIN_BENEFICIARIO = "44455566677";
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String URL_LISTA = "/rastreabilidade/faturas";
    private static final String URL_DETALHE = "/rastreabilidade/faturas/{id}";

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
        nova.setNome("Laboratorio Consulta Lisura");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-CONS-LISURA");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_RASTREABILIDADE, PerfilUsuario.RASTREABILIDADE);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Joao Consulta");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void listaTrazEmLisuraEAguardandoOcsEmOrdemDeEnvio() throws Exception {
        novoEspelho(SituacaoFatura.EM_LISURA, LocalDateTime.of(2026, 11, 12, 9, 0), "002/2026");
        Espelho aguardando = novoEspelho(SituacaoFatura.AGUARDANDO_OCS, LocalDateTime.of(2026, 11, 10, 9, 0),
                "001/2026");
        registrarEvento(aguardando, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.of(2026, 11, 11, 9, 0));
        novoEspelho(SituacaoFatura.PROTOCOLADA, LocalDateTime.of(2026, 11, 9, 9, 0), "003/2026");
        novoEspelho(SituacaoFatura.EM_AUDITORIA, LocalDateTime.of(2026, 11, 8, 9, 0), "004/2026");
        novoEspelho(SituacaoFatura.AUDITADA, LocalDateTime.of(2026, 11, 7, 9, 0), "005/2026");

        mockMvc.perform(get(URL_LISTA).header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].situacao").value("AGUARDANDO_OCS"))
                .andExpect(jsonPath("$[0].dataEnvioOcs").value("2026-11-11T09:00:00"))
                .andExpect(jsonPath("$[1].situacao").value("EM_LISURA"));
    }

    @Test
    void linhasTragComparacaoGlosaEObservacaoSemTotalFinalAteDecidirTudo() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, LocalDateTime.of(2026, 11, 10, 9, 0), "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), 2, new BigDecimal("100.00"),
                OrigemContrato.CONFERIDO, null);
        novoItem(espelho, "41001102", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL, "Conferir com a OCS");
        novoItem(espelho, "41001103", new BigDecimal("100.00"), 1, new BigDecimal("120.00"),
                OrigemContrato.TABELA_CORRIGIDA, null);
        novoItem(espelho, "41001104", new BigDecimal("100.00"), 1, null, null, null);

        mockMvc.perform(get(URL_DETALHE, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas", hasSize(4)))
                .andExpect(jsonPath("$.linhas[0].comparacao").value("CONFORME"))
                .andExpect(jsonPath("$.linhas[0].glosa", closeTo(0.0, 0.001)))
                .andExpect(jsonPath("$.linhas[1].comparacao").value("ACIMA"))
                .andExpect(jsonPath("$.linhas[1].glosa", closeTo(20.0, 0.001)))
                .andExpect(jsonPath("$.linhas[1].origemContrato").value("MANUAL"))
                .andExpect(jsonPath("$.linhas[1].observacao").value("Conferir com a OCS"))
                .andExpect(jsonPath("$.linhas[2].comparacao").value("ABAIXO"))
                .andExpect(jsonPath("$.linhas[2].glosa", closeTo(-20.0, 0.001)))
                .andExpect(jsonPath("$.linhas[3].comparacao").value("SEM_CONTRATO"))
                .andExpect(jsonPath("$.linhas[3].glosa").value(nullValue()))
                .andExpect(jsonPath("$.totais.informado", closeTo(500.0, 0.001)))
                .andExpect(jsonPath("$.totais.glosa").value(nullValue()))
                .andExpect(jsonPath("$.totais.final").value(nullValue()));
    }

    @Test
    void totalFinalEGlosaAparecemQuandoTodasAsLinhasEstaoDecididas() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, LocalDateTime.of(2026, 11, 10, 9, 0), "001/2026");
        novoItem(espelho, "41001101", new BigDecimal("100.00"), 2, new BigDecimal("90.00"),
                OrigemContrato.MANUAL, null);

        mockMvc.perform(get(URL_DETALHE, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totais.informado", closeTo(200.0, 0.001)))
                .andExpect(jsonPath("$.totais.final", closeTo(180.0, 0.001)))
                .andExpect(jsonPath("$.totais.glosa", closeTo(20.0, 0.001)));
    }

    @Test
    void emLisuraLiberaSoSemGlosaEComTodasDecididas() throws Exception {
        Espelho semGlosa = novoEspelho(SituacaoFatura.EM_LISURA, LocalDateTime.of(2026, 11, 5, 9, 0), "001/2026");
        novoItem(semGlosa, "41001101", new BigDecimal("100.00"), 1, new BigDecimal("100.00"),
                OrigemContrato.CONFERIDO, null);
        Espelho comGlosa = novoEspelho(SituacaoFatura.EM_LISURA, LocalDateTime.of(2026, 11, 6, 9, 0), "002/2026");
        novoItem(comGlosa, "41001102", new BigDecimal("100.00"), 1, new BigDecimal("80.00"),
                OrigemContrato.MANUAL, null);

        mockMvc.perform(get(URL_LISTA).header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroFatura").value("001/2026"))
                .andExpect(jsonPath("$[0].liberadaParaAuditoria").value(true))
                .andExpect(jsonPath("$[1].numeroFatura").value("002/2026"))
                .andExpect(jsonPath("$[1].liberadaParaAuditoria").value(false));
    }

    @Test
    void aguardandoOcsLiberaComPrazoVencidoEBloqueiaDentroDoPrazo() throws Exception {
        Espelho prazoVencido = novoEspelho(SituacaoFatura.AGUARDANDO_OCS,
                LocalDateTime.of(2000, 1, 1, 9, 0), "001/2026");
        registrarEvento(prazoVencido, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.of(2000, 1, 3, 9, 0));
        Espelho dentroDoPrazo = novoEspelho(SituacaoFatura.AGUARDANDO_OCS,
                LocalDateTime.of(2026, 11, 2, 9, 0), "002/2026");
        registrarEvento(dentroDoPrazo, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.now(Beneficiario.FUSO));

        mockMvc.perform(get(URL_LISTA).header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroFatura").value("001/2026"))
                .andExpect(jsonPath("$[0].liberadaParaAuditoria").value(true))
                .andExpect(jsonPath("$[1].numeroFatura").value("002/2026"))
                .andExpect(jsonPath("$[1].liberadaParaAuditoria").value(false));
    }

    @Test
    void detalheDeFaturaProtocoladaRecebe404MasDeEmAuditoriaRecebeOk() throws Exception {
        Espelho protocolada = novoEspelho(SituacaoFatura.PROTOCOLADA, LocalDateTime.of(2026, 11, 10, 9, 0), "001/2026");
        Espelho emAuditoria = novoEspelho(SituacaoFatura.EM_AUDITORIA, LocalDateTime.of(2026, 11, 10, 9, 0),
                "002/2026");

        mockMvc.perform(get(URL_DETALHE, protocolada.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(URL_DETALHE, emAuditoria.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("EM_AUDITORIA"));
    }

    @Test
    void perfilSemPermissaoRecebe403() throws Exception {
        Espelho espelho = novoEspelho(SituacaoFatura.EM_LISURA, LocalDateTime.of(2026, 11, 10, 9, 0), "001/2026");

        mockMvc.perform(get(URL_LISTA).header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL_DETALHE, espelho.getIdEspelho()).header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isForbidden());
    }

    private Espelho novoEspelho(SituacaoFatura situacao, LocalDateTime envio, String numero) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(envio);
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura(numero);
        espelho.setProtocoladaPor(setor);
        return espelhoRepository.save(espelho);
    }

    private void registrarEvento(Espelho espelho, TipoFaturaEvento tipo, LocalDateTime data) {
        FaturaEvento evento = new FaturaEvento();
        evento.setEspelho(espelho);
        evento.setTipo(tipo);
        evento.setUsuario(setor);
        evento.setResponsavel("Ana Silva");
        evento.setDataEvento(data);
        eventoRepository.save(evento);
    }

    private EspelhoItem novoItem(Espelho espelho, String codDgp, BigDecimal valorSessao, int quantidade,
                                 BigDecimal contrato, OrigemContrato origem, String observacao) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(), codDgp));
        item.setQuantidadeSessoes(quantidade);
        item.setValorSessao(valorSessao);
        item.setValorContratoSessao(contrato);
        item.setOrigemContrato(origem);
        item.setObservacao(observacao);
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