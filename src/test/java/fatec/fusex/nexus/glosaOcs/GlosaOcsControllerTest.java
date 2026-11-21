package fatec.fusex.nexus.glosaOcs;

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
import fatec.fusex.nexus.ocs.ContaOcs;
import fatec.fusex.nexus.ocs.ContaOcsRepository;
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
class GlosaOcsControllerTest {

    private static final String LOGIN_OCS_A = "ocs.glosa.a";
    private static final String LOGIN_OCS_B = "ocs.glosa.b";
    private static final String LOGIN_SETOR = "setor.glosa";
    private static final String LOGIN_BENEFICIARIO = "66655544433";
    private static final String BASE = "/ocs/glosas";

    @Autowired private MockMvc mockMvc;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private ContaOcsRepository contaOcsRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private GuiaProcedimentoRepository procedimentoRepository;
    @Autowired private EspelhoRepository espelhoRepository;
    @Autowired private EspelhoItemRepository espelhoItemRepository;
    @Autowired private FaturaEventoRepository eventoRepository;

    private Ocs ocsA;
    private Usuario setor;
    private Beneficiario joao;
    private int sequencia;

    @BeforeEach
    void cenario() {
        ocsA = novaOcs("Laboratorio A", "CTR-GLOSA-A");
        Ocs ocsB = novaOcs("Laboratorio B", "CTR-GLOSA-B");
        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        vincularConta(LOGIN_OCS_A, ocsA);
        vincularConta(LOGIN_OCS_B, ocsB);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Joao Glosa");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void ocsVeSoASuaFaturaComGlosaPendente() throws Exception {
        novaFaturaComGlosa(ocsA, false);

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].valorGlosa").value(20.00));

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS_B))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void detalheDeOutraOcsRecebe403() throws Exception {
        Espelho espelho = novaFaturaComGlosa(ocsA, false);

        mockMvc.perform(get(BASE + "/{id}", espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS_B))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalheTrazLinhasComGlosaESemCiencia() throws Exception {
        Espelho espelho = novaFaturaComGlosa(ocsA, false);

        mockMvc.perform(get(BASE + "/{id}", espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guiasGlosadas.length()").value(1))
                .andExpect(jsonPath("$.dataCiencia").doesNotExist());
    }

    @Test
    void rodadaDecididaSaiDaLista() throws Exception {
        novaFaturaComGlosa(ocsA, true);

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS_A))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void faturaSemGlosaNaoAparece() throws Exception {
        Espelho espelho = novaFaturaSemGlosa(ocsA);

        mockMvc.perform(get(BASE + "/{id}", espelho.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS_A))
                .andExpect(status().isNotFound());
    }

    private Espelho novaFaturaComGlosa(Ocs ocs, boolean decidida) {
        Espelho espelho = novoEspelho(ocs, SituacaoFatura.AGUARDANDO_OCS);
        // Contrato 90 contra informado 100: glosa de 10 por sessao x 2 sessoes = 20.
        novoItem(espelho, new BigDecimal("90.00"), new BigDecimal("100.00"));
        eventoRepository.save(evento(espelho, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.of(2026, 11, 10, 9, 0)));
        if (decidida) {
            eventoRepository.save(evento(espelho, TipoFaturaEvento.CIENTE_OCS, LocalDateTime.of(2026, 11, 11, 9, 0)));
        }
        return espelho;
    }

    private Espelho novaFaturaSemGlosa(Ocs ocs) {
        Espelho espelho = novoEspelho(ocs, SituacaoFatura.AGUARDANDO_OCS);
        novoItem(espelho, new BigDecimal("100.00"), new BigDecimal("100.00"));
        eventoRepository.save(evento(espelho, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.of(2026, 11, 10, 9, 0)));
        return espelho;
    }

    private FaturaEvento evento(Espelho espelho, TipoFaturaEvento tipo, LocalDateTime quando) {
        FaturaEvento evento = new FaturaEvento();
        evento.setEspelho(espelho);
        evento.setTipo(tipo);
        evento.setUsuario(setor);
        evento.setResponsavel("Sgt Ribeiro");
        evento.setDataEvento(quando);
        return evento;
    }

    private Espelho novoEspelho(Ocs ocs, SituacaoFatura situacao) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(LocalDateTime.of(2026, 11, 9, 9, 0));
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura("0" + (++sequencia) + "0/2026");
        espelho.setProtocoladaPor(setor);
        return espelhoRepository.save(espelho);
    }

    private EspelhoItem novoItem(Espelho espelho, BigDecimal contrato, BigDecimal informado) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(espelho);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(), "41001300"));
        item.setQuantidadeSessoes(2);
        item.setValorSessao(informado);
        item.setOrigemContrato(OrigemContrato.TABELA);
        item.setValorContratoSessao(contrato);
        return espelhoItemRepository.save(item);
    }

    private Guia novaGuia() {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setBeneficiario(joao);
        preGuia.setOcs(ocsA);
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

    private GuiaProcedimento novoProcedimento(Guia guia, String codDgp) {
        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guia);
        procedimento.setCodDgp(codDgp);
        procedimento.setDescricaoProcedimento("Exame de glosa");
        procedimento.setQuantidadeAutorizada(2);
        return procedimentoRepository.save(procedimento);
    }

    private Ocs novaOcs(String nome, String contrato) {
        Ocs ocs = new Ocs();
        ocs.setNome(nome);
        ocs.setTipo(OcsTipo.OCS);
        ocs.setContratoNum(contrato);
        return ocsRepository.save(ocs);
    }

    private void vincularConta(String login, Ocs ocs) {
        Usuario usuario = novoUsuario(login, PerfilUsuario.OCS);
        ContaOcs conta = new ContaOcs();
        conta.setUsuario(usuario);
        conta.setOcs(ocs);
        contaOcsRepository.save(conta);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }
}
