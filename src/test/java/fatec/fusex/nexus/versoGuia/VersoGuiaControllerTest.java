package fatec.fusex.nexus.versoGuia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.EspelhoItemRepository;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcs;
import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcsRepository;
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

/** T32-BE1: lista, Guias e PDF dos versos das Guias glosadas (regra 15). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VersoGuiaControllerTest {

    private static final String LOGIN = "rastreabilidade.versos";
    private static final String BASE = "/rastreabilidade/versos";
    private static final LocalDateTime ENVIO_ANTIGO = LocalDateTime.of(2026, 11, 11, 9, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private DependenteRepository dependenteRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private GuiaProcedimentoRepository procedimentoRepository;
    @Autowired private EspelhoRepository espelhoRepository;
    @Autowired private EspelhoItemRepository espelhoItemRepository;
    @Autowired private EspelhoItemRodadaOcsRepository rodadaRepository;
    @Autowired private FaturaEventoRepository eventoRepository;

    private Ocs ocs;
    private Usuario usuario;
    private Beneficiario titular;
    private Dependente miguel;
    private int sequencia;

    @BeforeEach
    void cenario() {
        Ocs nova = new Ocs();
        nova.setNome("Laboratorio Versos");
        nova.setTipo(OcsTipo.OCS);
        nova.setContratoNum("CTR-VERSOS-1");
        ocs = ocsRepository.save(nova);
        usuario = novoUsuario(LOGIN, PerfilUsuario.RASTREABILIDADE);
        novoUsuario("auditoria.versos", PerfilUsuario.AUDITORIA);

        Beneficiario b = new Beneficiario();
        b.setUsuario(novoUsuario("88877766655", PerfilUsuario.BENEFICIARIO));
        b.setNome("Antonio Versos");
        b.setPrecCp("88877766655");
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("11999999999");
        titular = beneficiarioRepository.save(b);

        Dependente d = new Dependente();
        d.setTitular(titular);
        d.setNome("Miguel Versos");
        d.setDataNascimento(LocalDate.of(2020, 3, 21));
        d.setCpf("39053344781");
        d.setPrecCp("88877766601");
        d.setTipo(TipoDependente.MENOR_TITULAR);
        d.setVigenciaFim(LocalDate.of(2042, 3, 20));
        miguel = dependenteRepository.save(d);
    }

    @Test
    void faturaEmAuditoriaListaSoAsGuiasQueForamAOcs() throws Exception {
        Espelho fatura = fatura(SituacaoFatura.EM_AUDITORIA, "021/2026");
        FaturaEvento envio = evento(fatura, TipoFaturaEvento.ENVIADA_OCS, ENVIO_ANTIGO);
        FaturaEvento ok = evento(fatura, TipoFaturaEvento.CIENTE_OCS, ENVIO_ANTIGO.plusDays(1));
        rodada(item(fatura, null, "80.00"), envio, ok);
        rodada(item(fatura, miguel, "70.00"), envio, ok);
        item(fatura, null, "100.00");

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guiasGlosadas").value(2))
                .andExpect(jsonPath("$[0].valorGlosa").value(50.00));

        mockMvc.perform(get(BASE + "/{id}", fatura.getIdEspelho()).header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guias.length()").value(2))
                .andExpect(jsonPath("$.guias[0].seloFinal").value("Aceita pela OCS"))
                .andExpect(jsonPath("$.guias[1].paciente").value("Miguel Versos"));
    }

    @Test
    void faturaEmLisuraNaoTemVerso() throws Exception {
        Espelho fatura = fatura(SituacaoFatura.EM_LISURA, "022/2026");
        FaturaEvento envio = evento(fatura, TipoFaturaEvento.ENVIADA_OCS, ENVIO_ANTIGO);
        rodada(item(fatura, null, "80.00"), envio, null);

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(BASE + "/{id}", fatura.getIdEspelho()).header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(status().isConflict());
    }

    @Test
    void aguardandoOcsDentroDoPrazoNaoTemVersoEComPrazoVencidoTem() throws Exception {
        Espelho noPrazo = fatura(SituacaoFatura.AGUARDANDO_OCS, "023/2026");
        rodada(item(noPrazo, null, "80.00"), evento(noPrazo, TipoFaturaEvento.ENVIADA_OCS, LocalDateTime.now()), null);
        Espelho vencida = fatura(SituacaoFatura.AGUARDANDO_OCS, "024/2026");
        EspelhoItem item = item(vencida, null, "80.00");
        rodada(item, evento(vencida, TipoFaturaEvento.ENVIADA_OCS, ENVIO_ANTIGO), null);

        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroFatura").value("024/2026"));

        byte[] pdf = mockMvc.perform(get(BASE + "/{f}/guias/{g}/pdf", vencida.getIdEspelho(), guiaId(item))
                        .header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = texto(pdf);
        assertThat(texto).contains("Guia nº " + guiaNumero(item), "Fatura nº 024/2026", "Prazo vencido",
                "Prazo vencido sem resposta da OCS. A glosa segue como lisurada pela rastreabilidade.",
                "Imprimir no verso da Guia nº " + guiaNumero(item));
        assertThat(texto).doesNotContain("Assinatura");
    }

    @Test
    void pdfDaFaturaTemUmaPaginaPorGuiaNaOrdemDoNumero() throws Exception {
        Espelho fatura = fatura(SituacaoFatura.AUDITADA, "025/2026");
        FaturaEvento envio = evento(fatura, TipoFaturaEvento.ENVIADA_OCS, ENVIO_ANTIGO);
        FaturaEvento ok = evento(fatura, TipoFaturaEvento.CIENTE_OCS, ENVIO_ANTIGO.plusDays(1));
        EspelhoItem primeira = item(fatura, null, "80.00");
        EspelhoItem segunda = item(fatura, miguel, "70.00");
        rodada(primeira, envio, ok);
        rodada(segunda, envio, ok);

        byte[] pdf = mockMvc.perform(get(BASE + "/{f}/pdf", fatura.getIdEspelho())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getNumberOfPages()).isEqualTo(2);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String segundaPagina = stripper.getText(documento);
            assertThat(segundaPagina).contains("Guia nº " + guiaNumero(segunda), "Paciente: Miguel Versos",
                    "Titular: Antonio Versos");
        }
    }

    @Test
    void guiaQueNuncaFoiAOcsRecebe404EOutroPerfil403() throws Exception {
        Espelho fatura = fatura(SituacaoFatura.EM_AUDITORIA, "026/2026");
        FaturaEvento envio = evento(fatura, TipoFaturaEvento.ENVIADA_OCS, ENVIO_ANTIGO);
        rodada(item(fatura, null, "80.00"), envio, evento(fatura, TipoFaturaEvento.CIENTE_OCS, ENVIO_ANTIGO.plusDays(1)));
        EspelhoItem semGlosa = item(fatura, null, "100.00");

        mockMvc.perform(get(BASE + "/{f}/guias/{g}/pdf", fatura.getIdEspelho(), guiaId(semGlosa))
                        .header(SimulacaoLoginFilter.HEADER, LOGIN))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE).header(SimulacaoLoginFilter.HEADER, "auditoria.versos"))
                .andExpect(status().isForbidden());
    }

    // ---------- fixtures ----------

    private static String texto(byte[] pdf) throws Exception {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    private static Long guiaId(EspelhoItem item) {
        return item.getGuiaProcedimento().getGuia().getIdGuia();
    }

    private static String guiaNumero(EspelhoItem item) {
        return item.getGuiaProcedimento().getGuia().getNumero();
    }

    private Espelho fatura(SituacaoFatura situacao, String numero) {
        Espelho espelho = new Espelho();
        espelho.setOcs(ocs);
        espelho.setStatus(StatusEspelho.ENVIADO);
        espelho.setCriadoPor(usuario);
        espelho.setDataCriacao(LocalDateTime.of(2026, 11, 1, 8, 0));
        espelho.setDataEnvio(LocalDateTime.of(2026, 11, 2, 9, 0));
        espelho.setSituacaoFatura(situacao);
        espelho.setNumeroFatura(numero);
        espelho.setProtocoladaPor(usuario);
        return espelhoRepository.save(espelho);
    }

    /** Linha com informado de R$ 100,00 por sessao, uma sessao, e o contrato informado. */
    private EspelhoItem item(Espelho fatura, Dependente dependente, String contrato) {
        EspelhoItem item = new EspelhoItem();
        item.setEspelho(fatura);
        item.setGuiaProcedimento(novoProcedimento(novaGuia(dependente)));
        item.setQuantidadeSessoes(1);
        item.setValorSessao(new BigDecimal("100.00"));
        item.setValorContratoSessao(new BigDecimal(contrato));
        item.setOrigemContrato(OrigemContrato.MANUAL);
        item.setObservacao("Conferido com o contrato vigente");
        return espelhoItemRepository.save(item);
    }

    private void rodada(EspelhoItem item, FaturaEvento envio, FaturaEvento ok) {
        EspelhoItemRodadaOcs rodada = new EspelhoItemRodadaOcs();
        rodada.setEspelhoItem(item);
        rodada.setEnvioEvento(envio);
        rodada.setValorContratoSessao(item.getValorContratoSessao());
        rodada.setOrigemContrato(OrigemContrato.MANUAL);
        if (ok != null) {
            rodada.setDecisaoOcs("OK");
            rodada.setDecisaoEvento(ok);
        }
        rodadaRepository.save(rodada);
    }

    private FaturaEvento evento(Espelho fatura, TipoFaturaEvento tipo, LocalDateTime data) {
        FaturaEvento evento = new FaturaEvento();
        evento.setEspelho(fatura);
        evento.setTipo(tipo);
        evento.setUsuario(usuario);
        evento.setResponsavel("Ana Silva");
        evento.setDataEvento(data);
        return eventoRepository.save(evento);
    }

    private Guia novaGuia(Dependente dependente) {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setBeneficiario(titular);
        preGuia.setDependente(dependente);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamento.pdf");
        preGuia = preGuiaRepository.save(preGuia);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(String.format("%09d", 900000 + ++sequencia));
        guia.setArquivoUrl("guia.pdf");
        guia.setDataInsercao(LocalDateTime.of(2026, 9, 1, 9, 0));
        guia.setUsuario(usuario);
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
        guia.setSituacaoExecucao(SituacaoExecucao.REALIZADA);
        return guiaRepository.save(guia);
    }

    private GuiaProcedimento novoProcedimento(Guia guia) {
        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guia);
        procedimento.setCodDgp("41001101");
        procedimento.setDescricaoProcedimento("TC - Abdome superior");
        procedimento.setQuantidadeAutorizada(2);
        return procedimentoRepository.save(procedimento);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash-de-teste");
        u.setPerfil(perfil);
        return usuarioRepository.save(u);
    }
}
