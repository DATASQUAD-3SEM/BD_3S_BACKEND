package fatec.fusex.nexus.setorFatura;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
class FaturaConsultaControllerTest {

    private static final String LOGIN_SETOR = "setor.consulta";
    private static final String LOGIN_OCS = "ocs.consulta";
    private static final String LOGIN_BENEFICIARIO = "22233344455";
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final LocalDateTime ENVIO_RECEBIDO = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final LocalDateTime ENVIO_PROTOCOLADO = LocalDateTime.of(2026, 11, 12, 9, 0);
    private static final LocalDateTime ENVIO_AUDITADO = LocalDateTime.of(2026, 11, 5, 9, 0);

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
        nova.setNome("Laboratorio Consulta");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-CONS-1");
        ocs = ocsRepository.save(nova);

        setor = novoUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_FATURA);
        novoUsuario(LOGIN_OCS, PerfilUsuario.OCS);

        Usuario usuarioJoao = novoUsuario(LOGIN_BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioJoao);
        beneficiario.setNome("Maria Consulta");
        beneficiario.setPrecCp(LOGIN_BENEFICIARIO);
        beneficiario.setDataNascimento(LocalDate.of(1980, 1, 1));
        beneficiario.setTelefone("11999999999");
        joao = beneficiarioRepository.save(beneficiario);
    }

    @Test
    void listaSoTrazFaturasNaoAuditadas() throws Exception {
        novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.RECEBIDO, ENVIO_RECEBIDO, null);
        novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.AUDITADA, ENVIO_AUDITADO, "002/2026");
        novoEspelho(StatusEspelho.EM_CONSTRUCAO, null, null, null);
        novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.PROTOCOLADA, ENVIO_PROTOCOLADO, "001/2026");

        mockMvc.perform(get("/setor-fatura/faturas")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].situacao", containsInAnyOrder("RECEBIDO", "PROTOCOLADA")));
    }

    @Test
    void listaVemDoMaisRecenteParaOMaisAntigo() throws Exception {
        novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.RECEBIDO, ENVIO_RECEBIDO, null);
        novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.PROTOCOLADA, ENVIO_PROTOCOLADO, "001/2026");

        mockMvc.perform(get("/setor-fatura/faturas")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroFatura").value("001/2026"))
                .andExpect(jsonPath("$[1].numeroFatura").value(nullValue()));
    }

    @Test
    void listaTrazOPrazoDaOcsEACienciaDoEnvioMaisRecente() throws Exception {
        Espelho aguardando = novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.AGUARDANDO_OCS, ENVIO_RECEBIDO,
                "003/2026");
        // Envio numa quarta as 09:00: o prazo vai ate segunda as 23:59:59 (sabado e domingo nao contam).
        registrarEvento(aguardando, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.of(2026, 11, 11, 9, 0), "Ana Silva");
        registrarEvento(aguardando, TipoFaturaEvento.CIENTE_OCS, LocalDateTime.of(2026, 11, 12, 10, 0), "Bruno Lima");
        novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.PROTOCOLADA, ENVIO_PROTOCOLADO, "001/2026");

        mockMvc.perform(get("/setor-fatura/faturas")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.numeroFatura == '003/2026')].dataEnvioOcs").value("2026-11-11T09:00:00"))
                .andExpect(jsonPath("$[?(@.numeroFatura == '003/2026')].prazoOcs").value("2026-11-16T23:59:59"))
                .andExpect(jsonPath("$[?(@.numeroFatura == '003/2026')].dataCiencia").value("2026-11-12T10:00:00"))
                .andExpect(jsonPath("$[?(@.numeroFatura == '003/2026')].responsavelCiencia").value("Bruno Lima"))
                .andExpect(jsonPath("$[?(@.numeroFatura == '001/2026')].responsavelCiencia").value(org.hamcrest.Matchers.contains(nullValue())))
                .andExpect(jsonPath("$[?(@.numeroFatura == '001/2026')].prazoOcs").value(org.hamcrest.Matchers.contains(nullValue())));
    }

    @Test
    void detalheTrazLinhasTotalEventosEControleDeFluxo() throws Exception {
        Espelho protocolado = novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.PROTOCOLADA,
                ENVIO_PROTOCOLADO, "001/2026");
        Guia guia = novaGuiaComDoisExames();
        vincular(protocolado, guia);
        registrarEvento(protocolado, TipoFaturaEvento.PROTOCOLADA, LocalDateTime.of(2026, 11, 12, 10, 0), "Ana Silva");
        registrarEvento(protocolado, TipoFaturaEvento.ENVIADA_LISURA, LocalDateTime.of(2026, 11, 13, 10, 0),
                "Bruno Lima");

        mockMvc.perform(get("/setor-fatura/faturas/{id}", protocolado.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFatura").value("001/2026"))
                .andExpect(jsonPath("$.valorTotal", closeTo(300.0, 0.001)))
                .andExpect(jsonPath("$.linhas", hasSize(2)))
                .andExpect(jsonPath("$.linhas[0].guiaNumero").value(guia.getNumero()))
                .andExpect(jsonPath("$.totais.informado", closeTo(300.0, 0.001)))
                .andExpect(jsonPath("$.eventos", hasSize(2)))
                .andExpect(jsonPath("$.eventos[0].tipo").value("PROTOCOLADA"))
                .andExpect(jsonPath("$.eventos[0].usuario").value(LOGIN_SETOR))
                .andExpect(jsonPath("$.controleFluxo[0].etapa").value("Protocolo"))
                .andExpect(jsonPath("$.controleFluxo[1].etapa").value("Lisura / Glosa"));
    }

    @Test
    void detalheDeFaturaAuditadaAindaRetornaOk() throws Exception {
        Espelho auditado = novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.AUDITADA, ENVIO_AUDITADO, "002/2026");

        mockMvc.perform(get("/setor-fatura/faturas/{id}", auditado.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AUDITADA"));
    }

    @Test
    void espelhoEmConstrucaoOuInexistenteRecebe404NoDetalhe() throws Exception {
        Espelho emConstrucao = novoEspelho(StatusEspelho.EM_CONSTRUCAO, null, null, null);

        mockMvc.perform(get("/setor-fatura/faturas/{id}", emConstrucao.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/setor-fatura/faturas/{id}", 999999L)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_SETOR))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilSemPermissaoRecebe403NaListaENoDetalhe() throws Exception {
        Espelho protocolado = novoEspelho(StatusEspelho.ENVIADO, SituacaoFatura.PROTOCOLADA,
                ENVIO_PROTOCOLADO, "001/2026");

        mockMvc.perform(get("/setor-fatura/faturas")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/setor-fatura/faturas/{id}", protocolado.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isForbidden());
    }

    private Espelho novoEspelho(StatusEspelho status, SituacaoFatura situacao, LocalDateTime envio, String numero) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(status);
        espelho.setCriadoPor(setor);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(envio);
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura(numero);
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

    private Guia novaGuiaComDoisExames() {
        Guia guia = novaGuia();
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

    private GuiaProcedimento novoProcedimento(Guia guia, String codDgp, int quantidade) {
        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guia);
        procedimento.setCodDgp(codDgp);
        procedimento.setDescricaoProcedimento("TC - Abdome superior");
        procedimento.setQuantidadeAutorizada(quantidade);
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