package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2 + armazenamento) do PUT .../guia-rascunho. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AnexarGuiaControllerTest {

    private static final String SETOR = "setor.teste";
    private static final String BENEFICIARIO = "52998224725";
    private static final byte[] PDF = "%PDF-1.4 guia de teste".getBytes();

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final EntityManager entityManager;

    private Usuario setor;
    private Beneficiario beneficiario;
    private Ocs ocs;

    AnexarGuiaControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            GuiaRepository guiaRepository,
            GuiaRascunhoRepository guiaRascunhoRepository,
            ArmazenamentoArquivo armazenamentoArquivo,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void dados() {
        setor = usuario(SETOR, PerfilUsuario.SETOR_GUIAS);

        beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);

        ocs = new Ocs();
        ocs.setNome("Laboratorio Exemplo");
        ocs.setContratoNum("EXEMPLO-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.saveAndFlush(ocs);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private PreGuia preGuia(PreGuiaStatus status) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(beneficiario);
        p.setOcs(ocs);
        p.setStatus(status);
        p.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        return preGuiaRepository.saveAndFlush(p);
    }

    private ResultActions anexar(String login, Long id, String numero, MockMultipartFile arquivo) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart(HttpMethod.PUT, "/setor-guias/pre-guias/" + id + "/guia-rascunho");
        if (arquivo != null) {
            req.file(arquivo);
        }
        if (numero != null) {
            req.param("numeroGuia", numero);
        }
        return mockMvc.perform(req.header(SimulacaoLoginFilter.HEADER, login));
    }

    private static MockMultipartFile pdf(String nome) {
        return new MockMultipartFile("pdf", nome, "application/pdf", PDF);
    }

    private GuiaRascunho rascunhoNoBanco(Long id) {
        entityManager.flush();
        entityManager.clear();
        return guiaRascunhoRepository.findById(id).orElseThrow();
    }

    @Test
    void primeiroAnexo_gravaORascunhoSemCriarGuiaNemMudarStatus() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);

        anexar(SETOR, p.getIdPreGuia(), " 2026001234 ", pdf("guia-sire.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("2026001234"))
                .andExpect(jsonPath("$.nomeArquivo").value("guia-sire.pdf"))
                .andExpect(jsonPath("$.arquivoUrl").doesNotExist());

        GuiaRascunho r = rascunhoNoBanco(p.getIdPreGuia());
        assertThat(r.getNumero()).isEqualTo("2026001234");
        assertThat(r.getNomeArquivo()).isEqualTo("guia-sire.pdf");
        assertThat(armazenamentoArquivo.carregar(r.getArquivoUrl()).exists()).isTrue();
        assertThat(guiaRepository.findByPreGuiaIdPreGuia(p.getIdPreGuia())).isEmpty();
        assertThat(preGuiaRepository.findById(p.getIdPreGuia()).orElseThrow().getStatus())
                .isEqualTo(PreGuiaStatus.EM_ANALISE);
    }

    @Test
    void trocaDeArquivo_substituiORascunhoEDescartaOArquivoAnterior() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        anexar(SETOR, p.getIdPreGuia(), "2026001234", pdf("primeira.pdf")).andExpect(status().isOk());
        String arquivoAnterior = rascunhoNoBanco(p.getIdPreGuia()).getArquivoUrl();

        anexar(SETOR, p.getIdPreGuia(), "2026005678", pdf("segunda.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("2026005678"))
                .andExpect(jsonPath("$.nomeArquivo").value("segunda.pdf"));

        GuiaRascunho r = rascunhoNoBanco(p.getIdPreGuia());
        assertThat(r.getNumero()).isEqualTo("2026005678");
        assertThat(r.getArquivoUrl()).isNotEqualTo(arquivoAnterior);
        assertThatThrownBy(() -> armazenamentoArquivo.carregar(arquivoAnterior))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(guiaRascunhoRepository.count()).isEqualTo(1);
    }

    @Test
    void reenviarOMesmoNumeroNaPropriaPreGuia_naoEDuplicidade() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        anexar(SETOR, p.getIdPreGuia(), "2026001234", pdf("primeira.pdf")).andExpect(status().isOk());

        anexar(SETOR, p.getIdPreGuia(), "2026001234", pdf("corrigida.pdf")).andExpect(status().isOk());
    }

    @Test
    void numeroRepetido_emOutraGuiaOuRascunho_retorna409() throws Exception {
        // numero ja usado numa Guia concluida de outra pre-guia
        PreGuia comGuia = preGuia(PreGuiaStatus.CONCLUIDA);
        Guia guia = new Guia();
        guia.setPreGuia(comGuia);
        guia.setNumero("2026000001");
        guia.setArquivoUrl("guias/existente.pdf");
        guia.setUsuario(setor);
        guiaRepository.saveAndFlush(guia);

        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        anexar(SETOR, p.getIdPreGuia(), "2026000001", pdf("guia.pdf"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalhes[0]").value("numeroGuia: Numero de Guia ja usado em outra Guia"));

        // numero ja reservado no rascunho de outra pre-guia
        PreGuia outra = preGuia(PreGuiaStatus.EM_ANALISE);
        anexar(SETOR, outra.getIdPreGuia(), "2026000002", pdf("outra.pdf")).andExpect(status().isOk());
        anexar(SETOR, p.getIdPreGuia(), "2026000002", pdf("guia.pdf")).andExpect(status().isConflict());

        assertThat(guiaRascunhoRepository.findById(p.getIdPreGuia())).isEmpty();
    }

    @Test
    void arquivoQueNaoEPdf_retorna422() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);

        // extensao errada
        anexar(SETOR, p.getIdPreGuia(), "2026001234",
                new MockMultipartFile("pdf", "foto.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, 1, 2}))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("pdf: O arquivo da Guia deve ser um PDF"));
        // nome .pdf mas conteudo nao e PDF
        anexar(SETOR, p.getIdPreGuia(), "2026001234",
                new MockMultipartFile("pdf", "falso.pdf", "application/pdf", "nao sou pdf".getBytes()))
                .andExpect(status().isUnprocessableEntity());
        // sem arquivo
        anexar(SETOR, p.getIdPreGuia(), "2026001234", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("pdf: Anexe o PDF da Guia"));

        assertThat(guiaRascunhoRepository.findById(p.getIdPreGuia())).isEmpty();
    }

    @Test
    void numeroInvalido_retorna422() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);

        anexar(SETOR, p.getIdPreGuia(), "12A34", pdf("guia.pdf"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("numeroGuia: O numero da Guia deve ter so digitos"));
        anexar(SETOR, p.getIdPreGuia(), "  ", pdf("guia.pdf"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("numeroGuia: Informe o numero da Guia"));
        anexar(SETOR, p.getIdPreGuia(), "1".repeat(21), pdf("guia.pdf"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void statusQueNaoPermiteOuGuiaJaConcluida_retorna409() throws Exception {
        for (PreGuiaStatus status : new PreGuiaStatus[] {PreGuiaStatus.PENDENTE, PreGuiaStatus.ATENCAO, PreGuiaStatus.CONCLUIDA}) {
            PreGuia p = preGuia(status);
            anexar(SETOR, p.getIdPreGuia(), "2026001234", pdf("guia.pdf")).andExpect(status().isConflict());
        }

        // Em analise, mas a Guia ja foi concluida (aguardando assinaturas)
        PreGuia comGuia = preGuia(PreGuiaStatus.EM_ANALISE);
        Guia guia = new Guia();
        guia.setPreGuia(comGuia);
        guia.setNumero("2026009999");
        guia.setArquivoUrl("guias/existente.pdf");
        guia.setUsuario(setor);
        guiaRepository.saveAndFlush(guia);
        anexar(SETOR, comGuia.getIdPreGuia(), "2026001234", pdf("guia.pdf"))
                .andExpect(status().isConflict());
    }

    @Test
    void inexistente_retorna404() throws Exception {
        anexar(SETOR, 999999L, "2026001234", pdf("guia.pdf")).andExpect(status().isNotFound());
    }

    @Test
    void perfilErrado_retorna403SemGravar() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);

        anexar(BENEFICIARIO, p.getIdPreGuia(), "2026001234", pdf("guia.pdf")).andExpect(status().isForbidden());

        assertThat(guiaRascunhoRepository.findById(p.getIdPreGuia())).isEmpty();
    }

    @Test
    void semLogin_retorna401() throws Exception {
        PreGuia p = preGuia(PreGuiaStatus.EM_ANALISE);
        mockMvc.perform(multipart(HttpMethod.PUT, "/setor-guias/pre-guias/" + p.getIdPreGuia() + "/guia-rascunho")
                        .file(pdf("guia.pdf")).param("numeroGuia", "2026001234"))
                .andExpect(status().isUnauthorized());
    }
}
