package fatec.fusex.nexus.minhasPreGuias;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de GET /beneficiarios/me/pre-guias (T1-BE1) e do PDF da Guia do beneficiario (T1-BE2), com banco em memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class MinhasPreGuiasControllerTest {

    private final MockMvc mockMvc;
    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoFusexRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final GuiaRepository guiaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    private Beneficiario joao;
    private Beneficiario maria;
    private Ocs ocsPadrao;
    private MedicoFusex medicoFusex;
    private Usuario usuarioJoao;

    private Long idPreGuiaMaria;
    private Long idPreGuiaConcluidaJoao;
    private Long idPreGuiaPendenteJoao;

    private static final LocalDateTime D1 = LocalDateTime.of(2026, 10, 22, 10, 0);
    private static final LocalDateTime D2 = LocalDateTime.of(2026, 10, 21, 10, 0);
    private static final LocalDateTime D3 = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime D4 = LocalDateTime.of(2026, 10, 19, 10, 0);
    private static final LocalDateTime D5 = LocalDateTime.of(2026, 10, 18, 10, 0);

    MinhasPreGuiasControllerTest(
            MockMvc mockMvc,
            PreGuiaRepository preGuiaRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            UsuarioRepository usuarioRepository,
            MedicoFusexRepository medicoFusexRepository,
            PreGuiaHistoricoRepository historicoRepository,
            GuiaRepository guiaRepository,
            AssinaturaGuiaRepository assinaturaGuiaRepository,
            ArmazenamentoArquivo armazenamentoArquivo) {
        this.mockMvc = mockMvc;
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.usuarioRepository = usuarioRepository;
        this.medicoFusexRepository = medicoFusexRepository;
        this.historicoRepository = historicoRepository;
        this.guiaRepository = guiaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @BeforeEach
    void setUp() {
        usuarioJoao = new Usuario();
        usuarioJoao.setLogin("11111111111");
        usuarioJoao.setSenhaHash("hash");
        usuarioJoao.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuarioJoao.setAtivo(true);
        usuarioJoao = usuarioRepository.save(usuarioJoao);

        joao = new Beneficiario();
        joao.setUsuario(usuarioJoao);
        joao.setNome("João Logado");
        joao.setPrecCp("11111111111");
        joao.setDataNascimento(LocalDate.of(1990, 1, 1));
        joao.setTelefone("11999999991");
        joao = beneficiarioRepository.save(joao);

        Usuario usuarioMaria = new Usuario();
        usuarioMaria.setLogin("22222222222");
        usuarioMaria.setSenhaHash("hash");
        usuarioMaria.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuarioMaria.setAtivo(true);
        usuarioMaria = usuarioRepository.save(usuarioMaria);

        maria = new Beneficiario();
        maria.setUsuario(usuarioMaria);
        maria.setNome("Maria Outra");
        maria.setPrecCp("22222222222");
        maria.setDataNascimento(LocalDate.of(1985, 5, 20));
        maria.setTelefone("11999999992");
        maria = beneficiarioRepository.save(maria);

        Usuario usuarioMedico = new Usuario();
        usuarioMedico.setLogin("33333333333");
        usuarioMedico.setSenhaHash("hash");
        usuarioMedico.setPerfil(PerfilUsuario.MEDICO);
        usuarioMedico.setAtivo(true);
        usuarioMedico = usuarioRepository.save(usuarioMedico);

        medicoFusex = new MedicoFusex();
        medicoFusex.setUsuario(usuarioMedico);
        medicoFusex.setNome("Dr. Carlos");
        medicoFusex.setPrecCp("33333333333");
        medicoFusex.setPatente("Capitão");
        medicoFusex.setCrm("123456-SP");
        medicoFusex = medicoFusexRepository.save(medicoFusex);

        ocsPadrao = new Ocs();
        ocsPadrao.setNome("Hospital Central");
        ocsPadrao.setContratoNum("123456789");
        ocsPadrao.setTipo(OcsTipo.OCS);
        ocsPadrao = ocsRepository.save(ocsPadrao);

        // CONCLUIDA + Guia (PDF real) + 2 assinaturas
        PreGuia concluida = criarPreGuia(joao, PreGuiaStatus.CONCLUIDA, D1, null);
        Guia guia = new Guia();
        guia.setPreGuia(concluida);
        guia.setNumero("00042");
        guia.setArquivoUrl(salvarPdfFake("guia.pdf"));
        guia.setDataInsercao(D1);
        guia.setUsuario(usuarioJoao);
        guiaRepository.save(guia);
        idPreGuiaConcluidaJoao = concluida.getIdPreGuia();

        AssinaturaGuia aMed = new AssinaturaGuia();
        aMed.setGuia(guia);
        aMed.setPapel(AssinaturaGuiaPapel.MEDICO);
        aMed.setUsuario(usuarioJoao);
        assinaturaGuiaRepository.save(aMed);

        AssinaturaGuia aChefe = new AssinaturaGuia();
        aChefe.setGuia(guia);
        aChefe.setPapel(AssinaturaGuiaPapel.CHEFE);
        aChefe.setUsuario(usuarioJoao);
        assinaturaGuiaRepository.save(aChefe);

        // ATENCAO origem MEDICO + histórico
        PreGuia atencaoMedico = criarPreGuia(joao, PreGuiaStatus.ATENCAO, D2, medicoFusex);
        criarHistorico(atencaoMedico, usuarioJoao, "Documento do médico ilegível, reenviar");

        // ATENCAO origem BENEFICIARIO + histórico
        PreGuia atencaoSo = criarPreGuia(joao, PreGuiaStatus.ATENCAO, D3, null);
        criarHistorico(atencaoSo, usuarioJoao, "Faltou assinatura, corrigir");

        // PENDENTE
        PreGuia pendente = criarPreGuia(joao, PreGuiaStatus.PENDENTE, D4, null);
        idPreGuiaPendenteJoao = pendente.getIdPreGuia();

        // EM_ANALISE
        criarPreGuia(joao, PreGuiaStatus.EM_ANALISE, D5, null);

        // Maria (não deve aparecer para o João)
        PreGuia daMaria = criarPreGuia(maria, PreGuiaStatus.PENDENTE, D1, null);
        idPreGuiaMaria = daMaria.getIdPreGuia();
    }

    private PreGuia criarPreGuia(Beneficiario b, PreGuiaStatus s, LocalDateTime data, MedicoFusex medico) {
        PreGuia pg = new PreGuia();
        pg.setBeneficiario(b);
        pg.setOcs(ocsPadrao);
        pg.setStatus(s);
        pg.setDataCriacao(data);
        pg.setEncaminhamentoUrl("encaminhamentos/x.pdf");
        pg.setMedicoFusex(medico);
        return preGuiaRepository.save(pg);
    }

    private void criarHistorico(PreGuia pg, Usuario usuario, String observacao) {
        PreGuiaHistorico h = new PreGuiaHistorico(
                null,
                pg,
                PreGuiaStatus.ATENCAO,
                observacao,
                "encaminhamentos/x.pdf",
                usuario,
                LocalDateTime.now()
        );
        historicoRepository.save(h);
    }

    private String salvarPdfFake(String nome) {
        byte[] bytes = "%PDF-1.4\nfake\n%%EOF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile f = new MockMultipartFile("arquivo", nome, "application/pdf", bytes);
        return armazenamentoArquivo.salvar(f, "guias");
    }

    // ========== T1-BE1 ==========

    @Test
    @DisplayName("Sem header X-Usuario-Login retorna 401")
    void semHeader_retorna401() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Perfil não-BENEFICIARIO retorna 403")
    void perfilErrado_retorna403() throws Exception {
        Usuario setor = new Usuario();
        setor.setLogin("setor.guias");
        setor.setSenhaHash("hash");
        setor.setPerfil(PerfilUsuario.SETOR_GUIAS);
        setor.setAtivo(true);
        usuarioRepository.save(setor);

        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Beneficiário sem pré-guias recebe lista vazia")
    void listaVazia_retornaArrayVazio() throws Exception {
        Usuario u3 = new Usuario();
        u3.setLogin("44444444444");
        u3.setSenhaHash("hash");
        u3.setPerfil(PerfilUsuario.BENEFICIARIO);
        u3.setAtivo(true);
        u3 = usuarioRepository.save(u3);

        Beneficiario semGuias = new Beneficiario();
        semGuias.setUsuario(u3);
        semGuias.setNome("Sem Guias");
        semGuias.setPrecCp("44444444444");
        semGuias.setDataNascimento(LocalDate.of(2000, 1, 1));
        semGuias.setTelefone("11999999994");
        beneficiarioRepository.save(semGuias);

        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "44444444444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Lista vem ordenada por dataCriacao DESC")
    void ordenacaoPorData() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].status").value("CONCLUIDA"))
                .andExpect(jsonPath("$[1].status").value("ATENCAO"))
                .andExpect(jsonPath("$[2].status").value("ATENCAO"))
                .andExpect(jsonPath("$[3].status").value("PENDENTE"))
                .andExpect(jsonPath("$[4].status").value("EM_ANALISE"))
                .andExpect(jsonPath("$[0].dataCriacao").value("2026-10-22T10:00:00"))
                .andExpect(jsonPath("$[4].dataCriacao").value("2026-10-18T10:00:00"));
    }

    @Test
    @DisplayName("observacao só é preenchida quando status = ATENCAO")
    void observacaoApenasEmAtencao() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].observacao").doesNotExist())
                .andExpect(jsonPath("$[1].observacao").value("Documento do médico ilegível, reenviar"))
                .andExpect(jsonPath("$[2].observacao").value("Faltou assinatura, corrigir"))
                .andExpect(jsonPath("$[3].observacao").doesNotExist())
                .andExpect(jsonPath("$[4].observacao").doesNotExist());
    }

    @Test
    @DisplayName("origem MEDICO preenche criadoPorMedico com nome e patente")
    void origemMedicoPreencheDadosDoMedico() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origem").value("BENEFICIARIO"))
                .andExpect(jsonPath("$[0].criadoPorMedico").doesNotExist())
                .andExpect(jsonPath("$[1].origem").value("MEDICO"))
                .andExpect(jsonPath("$[1].criadoPorMedico.nome").value("Dr. Carlos"))
                .andExpect(jsonPath("$[1].criadoPorMedico.patente").value("Capitão"))
                .andExpect(jsonPath("$[2].origem").value("BENEFICIARIO"))
                .andExpect(jsonPath("$[2].criadoPorMedico").doesNotExist());
    }

    @Test
    @DisplayName("guia só é preenchida quando status = CONCLUIDA e validade = insercao + 2 meses")
    void guiaApenasEmConcluida() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guia.numero").value("00042"))
                .andExpect(jsonPath("$[0].guia.validadeAte").value("2026-12-22"))
                .andExpect(jsonPath("$[1].guia").doesNotExist())
                .andExpect(jsonPath("$[2].guia").doesNotExist())
                .andExpect(jsonPath("$[3].guia").doesNotExist())
                .andExpect(jsonPath("$[4].guia").doesNotExist());
    }

    @Test
    @DisplayName("Não expõe pré-guias de outro beneficiário")
    void isolamentoEntreBeneficiarios() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].id",
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.hasItem(idPreGuiaMaria.intValue()))));
    }

    // ========== T1-BE2 ==========

    @Test
    @DisplayName("PDF guia: 200 attachment com bytes do PDF")
    void pdfGuia_modoBaixar_retornaAttachment() throws Exception {
        var result = mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaConcluidaJoao)
                        .param("modo", "baixar")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("guia-00042.pdf")))
                .andReturn();

        byte[] corpo = result.getResponse().getContentAsByteArray();
        org.assertj.core.api.Assertions.assertThat(corpo)
                .startsWith("%PDF".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("PDF guia: 200 inline em modo visualizar")
    void pdfGuia_modoVisualizar_retornaInline() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaConcluidaJoao)
                        .param("modo", "visualizar")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("inline")));
    }

    @Test
    @DisplayName("PDF guia: 422 sem modo")
    void pdfGuia_semModo_retorna422() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaConcluidaJoao)
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PDF guia: 422 com modo invalido")
    void pdfGuia_modoInvalido_retorna422() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaConcluidaJoao)
                        .param("modo", "tocar")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PDF guia: 401 sem header")
    void pdfGuia_semHeader_retorna401() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaConcluidaJoao)
                        .param("modo", "baixar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PDF guia: 403 perfil errado")
    void pdfGuia_perfilErrado_retorna403() throws Exception {
        Usuario setor = new Usuario();
        setor.setLogin("setor.pdf");
        setor.setSenhaHash("hash");
        setor.setPerfil(PerfilUsuario.SETOR_GUIAS);
        setor.setAtivo(true);
        usuarioRepository.save(setor);

        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaConcluidaJoao)
                        .param("modo", "baixar")
                        .header(SimulacaoLoginFilter.HEADER, "setor.pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PDF guia: 403 pre-guia nao concluida")
    void pdfGuia_naoConcluida_retorna403() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaPendenteJoao)
                        .param("modo", "baixar")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PDF guia: 403 concluida com 1 assinatura")
    void pdfGuia_umaAssinatura_retorna403() throws Exception {
        PreGuia concluida1Ass = criarPreGuia(joao, PreGuiaStatus.CONCLUIDA, D1.minusMinutes(1), null);
        Guia guia1Ass = new Guia();
        guia1Ass.setPreGuia(concluida1Ass);
        guia1Ass.setNumero("00043");
        guia1Ass.setArquivoUrl(salvarPdfFake("guia1ass.pdf"));
        guia1Ass.setDataInsercao(D1.minusMinutes(1));
        guia1Ass.setUsuario(usuarioJoao);
        guiaRepository.save(guia1Ass);

        AssinaturaGuia aMed = new AssinaturaGuia();
        aMed.setGuia(guia1Ass);
        aMed.setPapel(AssinaturaGuiaPapel.MEDICO);
        aMed.setUsuario(usuarioJoao);
        assinaturaGuiaRepository.save(aMed);

        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf",
                        concluida1Ass.getIdPreGuia())
                        .param("modo", "baixar")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PDF guia: 404 pre-guia de outro beneficiario")
    void pdfGuia_deOutroBeneficiario_retorna404() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", idPreGuiaMaria)
                        .param("modo", "baixar")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PDF guia: 404 pre-guia inexistente")
    void pdfGuia_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", 99999L)
                        .param("modo", "baixar")
                        .header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isNotFound());
    }
}