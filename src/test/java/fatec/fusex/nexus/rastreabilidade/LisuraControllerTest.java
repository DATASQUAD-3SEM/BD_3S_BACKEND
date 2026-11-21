package fatec.fusex.nexus.rastreabilidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class LisuraControllerTest {

    private static final String LOGIN_RASTREABILIDADE = "rastreabilidade.lisura";
    private static final String RESPONSAVEL_LISURA = "Sgt Ribeiro";
    private static final String LOGIN_SETOR = "setor.lisura";
    private static final String LOGIN_BENEFICIARIO = "33344455566";
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String COD_TABELA = "41001109";
    private static final String COD_SEM_PRECO = "41001110";
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
    private OcsPrecoRepository ocsPrecoRepository;

    private Ocs ocs;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    @BeforeEach
    void cenario() {
        Ocs nova = new Ocs();
        nova.setNome("Laboratorio Lisura");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-LISURA-1");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_RASTREABILIDADE, PerfilUsuario.RASTREABILIDADE);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Joao Lisura");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void simNumaLinhaSemPrecoGravaConferidoSemCriarPreco() throws Exception {
        EspelhoItem item = novoItem(emLisura(), COD_SEM_PRECO, null, null);

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem() + ",\"conforme\":true}]"))
                .andExpect(status().isOk());

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.CONFERIDO);
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo(gravado.getValorSessao());
        assertThat(ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO)).isEmpty();
    }

    @Test
    void naoComValorNumaLinhaSemPrecoGravaManualECriaPreco() throws Exception {
        EspelhoItem item = novoItem(emLisura(), COD_SEM_PRECO, null, null);

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .param("responsavel", RESPONSAVEL_LISURA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem()
                                + ",\"conforme\":false,\"valorContrato\":80.00}]"))
                .andExpect(status().isOk());

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.MANUAL);
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo("80.00");
        OcsPreco preco = ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO).orElseThrow();
        assertThat(preco.getValorSessao()).isEqualByComparingTo("80.00");
        assertThat(preco.getAtualizadoPor()).isEqualTo(RESPONSAVEL_LISURA);
    }

    @Test
    void gravarPrecoSemResponsavelEhRecusadoSemMudarNada() throws Exception {
        EspelhoItem item = novoItem(emLisura(), COD_SEM_PRECO, null, null);

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem()
                                + ",\"conforme\":false,\"valorContrato\":80.00}]"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO)).isEmpty();
        assertThat(espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow().getOrigemContrato())
                .isNull();
    }

    @Test
    void corrigirTabelaAtualizaLinhaEPrecoSemDuplicar() throws Exception {
        ocsPrecoRepository.save(preco(COD_TABELA, "100.00"));
        EspelhoItem item = novoItem(emLisura(), COD_TABELA, OrigemContrato.TABELA, new BigDecimal("100.00"));

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .param("responsavel", RESPONSAVEL_LISURA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem()
                                + ",\"corrigirTabela\":true,\"valorContrato\":90.00}]"))
                .andExpect(status().isOk());

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.TABELA_CORRIGIDA);
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo("90.00");
        OcsPreco preco = ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_TABELA).orElseThrow();
        assertThat(preco.getValorSessao()).isEqualByComparingTo("90.00");
        assertThat(preco.getAtualizadoEm()).isNotNull();
        assertThat(preco.getAtualizadoPor()).isEqualTo(RESPONSAVEL_LISURA);
    }

    @Test
    void linhaDaTabelaSemCorrigirIgnoraConformeEValorEGravaObservacao() throws Exception {
        EspelhoItem item = novoItem(emLisura(), COD_TABELA, OrigemContrato.TABELA, new BigDecimal("100.00"));

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem()
                                + ",\"conforme\":false,\"valorContrato\":10.00,\"observacao\":\"Conferir com a OCS\"}]"))
                .andExpect(status().isOk());

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.TABELA);
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo("100.00");
        assertThat(gravado.getObservacao()).isEqualTo("Conferir com a OCS");
    }

    @Test
    void conformeNuloOuNaoDigitadoDeixaLinhaSemDecisaoSemAlterarPreco() throws Exception {
        EspelhoItem item = novoItem(emLisura(), COD_SEM_PRECO, null, null);

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem() + ",\"conforme\":null}]"))
                .andExpect(status().isOk());

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getOrigemContrato()).isNull();
        assertThat(gravado.getValorContratoSessao()).isNull();
        assertThat(ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO)).isEmpty();
    }

    @Test
    void simNumaLinhaContestadaGravaConferidoEAtualizaATabelaDePrecos() throws Exception {
        ocsPrecoRepository.save(preco(COD_SEM_PRECO, "80.00"));
        EspelhoItem item = contestada(novoItem(emLisura(), COD_SEM_PRECO, null, null));

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .param("responsavel", RESPONSAVEL_LISURA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem() + ",\"conforme\":true}]"))
                .andExpect(status().isOk());

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.CONFERIDO);
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo("100.00");
        OcsPreco preco = ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO).orElseThrow();
        assertThat(preco.getValorSessao()).isEqualByComparingTo("100.00");
        assertThat(preco.getAtualizadoPor()).isEqualTo(RESPONSAVEL_LISURA);
    }

    @Test
    void simNumaLinhaContestadaSemResponsavelEhRecusado() throws Exception {
        EspelhoItem item = contestada(novoItem(emLisura(), COD_SEM_PRECO, null, null));

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem() + ",\"conforme\":true}]"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO)).isEmpty();
        assertThat(espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow().getOrigemContrato())
                .isNull();
    }

    @Test
    void linhaAceitaPelaOcsNaoMudaOContratoSoAObservacao() throws Exception {
        EspelhoItem item = novoItem(emLisura(), COD_SEM_PRECO, OrigemContrato.MANUAL, new BigDecimal("80.00"));
        item.setDecisaoOcs("OK");
        espelhoItemRepository.save(item);

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .param("responsavel", RESPONSAVEL_LISURA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem()
                                + ",\"conforme\":false,\"valorContrato\":50.00,\"observacao\":\"Aceita pela OCS\"}]"))
                .andExpect(status().isOk());

        EspelhoItem gravado = espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow();
        assertThat(gravado.getOrigemContrato()).isEqualTo(OrigemContrato.MANUAL);
        assertThat(gravado.getValorContratoSessao()).isEqualByComparingTo("80.00");
        assertThat(gravado.getObservacao()).isEqualTo("Aceita pela OCS");
        assertThat(ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO)).isEmpty();
    }

    /** Linha que a OCS contestou: a T27-BE2 apaga o contrato e grava a decisao CONTESTADA com o motivo. */
    private EspelhoItem contestada(EspelhoItem item) {
        item.setDecisaoOcs("CONTESTADA");
        item.setMotivoContestacaoOcs("Valor diferente do contrato vigente");
        return espelhoItemRepository.save(item);
    }

    @Test
    void observacaoAcimaDe500CaracteresRecebe400ENadaEGravado() throws Exception {
        EspelhoItem item = novoItem(emLisura(), COD_SEM_PRECO, null, null);
        String longa = "a".repeat(501);

        mockMvc.perform(put(URL_LISURA, item.getEspelho().getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem()
                                + ",\"conforme\":true,\"observacao\":\"" + longa + "\"}]"))
                .andExpect(status().isBadRequest());

        assertThat(espelhoItemRepository.findById(item.getIdEspelhoItem()).orElseThrow().getObservacao()).isNull();
    }

    @Test
    void valorNegativoRecebe400EOutrasLinhasNaoSaoGravadas() throws Exception {
        Espelho espelho = emLisura();
        EspelhoItem negativo = novoItem(espelho, COD_SEM_PRECO, null, null);
        EspelhoItem conferido = novoItem(espelho, COD_TABELA, null, null);

        mockMvc.perform(put(URL_LISURA, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + conferido.getIdEspelhoItem() + ",\"conforme\":true},"
                                + "{\"itemId\":" + negativo.getIdEspelhoItem()
                                + ",\"conforme\":false,\"valorContrato\":-1.00}]"))
                .andExpect(status().isBadRequest());

        assertThat(espelhoItemRepository.findById(conferido.getIdEspelhoItem()).orElseThrow().getOrigemContrato())
                .isNull();
        assertThat(ocsPrecoRepository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), COD_SEM_PRECO)).isEmpty();
    }

    @Test
    void itemDeOutraFaturaRecebe400() throws Exception {
        EspelhoItem itemOutra = novoItem(emLisura(), COD_SEM_PRECO, null, null);
        Espelho espelho = emLisura();

        mockMvc.perform(put(URL_LISURA, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + itemOutra.getIdEspelhoItem() + ",\"conforme\":true}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void faturaQueNaoEstaEmLisuraRecebe409() throws Exception {
        Espelho protocolado = novoEspelho(SituacaoFatura.PROTOCOLADA);
        EspelhoItem item = novoItem(protocolado, COD_SEM_PRECO, null, null);

        mockMvc.perform(put(URL_LISURA, protocolado.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RASTREABILIDADE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem() + ",\"conforme\":true}]"))
                .andExpect(status().isConflict());
    }

    @Test
    void perfilSemPermissaoRecebe403() throws Exception {
        Espelho espelho = emLisura();
        EspelhoItem item = novoItem(espelho, COD_SEM_PRECO, null, null);

        mockMvc.perform(put(URL_LISURA, espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"itemId\":" + item.getIdEspelhoItem() + ",\"conforme\":true}]"))
                .andExpect(status().isForbidden());
    }

    private Espelho emLisura() {
        return novoEspelho(SituacaoFatura.EM_LISURA);
    }

    private Espelho novoEspelho(SituacaoFatura situacao) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(LocalDateTime.of(2026, 11, 10, 9, 0));
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura("001/2026");
        espelho.setProtocoladaPor(setor);
        return espelhoRepository.save(espelho);
    }

    private EspelhoItem novoItem(Espelho espelho, String codDgp, OrigemContrato origem, BigDecimal contrato) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(), codDgp));
        item.setQuantidadeSessoes(2);
        item.setValorSessao(new BigDecimal("100.00"));
        item.setOrigemContrato(origem);
        item.setValorContratoSessao(contrato);
        return espelhoItemRepository.save(item);
    }

    private OcsPreco preco(String codDgp, String valor) {
        OcsPreco preco = new OcsPreco();
        preco.setOcs(ocs);
        preco.setCodDgp(codDgp);
        preco.setValorSessao(new BigDecimal(valor));
        return preco;
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