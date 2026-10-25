package fatec.fusex.nexus.MinhasPreGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
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
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2 + armazenamento) do POST .../{id}/reenvio. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ReenvioPreGuiaControllerTest {

    private static final String BENEFICIARIO = "52998224725";
    private static final String OUTRO_BENEFICIARIO = "39053344705";
    private static final String MEDICO = "11144477735";
    private static final byte[] PDF = "%PDF-1.4 encaminhamento novo".getBytes();
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private MedicoFusexRepository medicoRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private PreGuiaHistoricoRepository historicoRepository;
    @Autowired private ArmazenamentoArquivo armazenamentoArquivo;
    @Autowired private EntityManager entityManager;

    private Usuario setor;
    private Beneficiario beneficiario;
    private Beneficiario outroBeneficiario;
    private MedicoFusex medico;
    private Ocs ocsAtual;
    private Ocs ocsNova;
    private Ocs ocsInativa;

    @BeforeEach
    void dados() {
        setor = usuario("setor.teste", PerfilUsuario.SETOR_GUIAS);
        beneficiario = beneficiario(BENEFICIARIO, "Antonio Carlos Ferreira", "45872213300");
        outroBeneficiario = beneficiario(OUTRO_BENEFICIARIO, "Marta Souza Lima", "45810020001");

        medico = new MedicoFusex();
        medico.setUsuario(usuario(MEDICO, PerfilUsuario.MEDICO));
        medico.setNome("Carlos Eduardo Lima");
        medico.setPatente("Capitao");
        medico.setPrecCp("11122233344");
        medico.setCrm("123456-SP");
        medicoRepository.saveAndFlush(medico);

        ocsAtual = ocs("Laboratorio Exemplo", "EXEMPLO-001", true);
        ocsNova = ocs("Hospital Exemplo", "EXEMPLO-002", true);
        ocsInativa = ocs("Clinica Inativa", "EXEMPLO-003", false);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Beneficiario beneficiario(String cpf, String nome, String precCp) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario(cpf, PerfilUsuario.BENEFICIARIO));
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("12987654321");
        return beneficiarioRepository.saveAndFlush(b);
    }

    private Ocs ocs(String nome, String contrato, boolean ativo) {
        Ocs o = new Ocs();
        o.setNome(nome);
        o.setContratoNum(contrato);
        o.setTipo(OcsTipo.OCS);
        o.setAtivo(ativo);
        return ocsRepository.saveAndFlush(o);
    }

    // Pre-guia do beneficiario em Atencao, com a observacao do setor no historico
    private Long preGuia(Beneficiario dono, MedicoFusex criador, PreGuiaStatus status) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(dono);
        p.setOcs(ocsAtual);
        p.setMedicoFusex(criador);
        p.setStatus(status);
        if (criador == null) {
            p.setEncaminhamentoUrl("encaminhamentos/antigo.jpg");
        }
        Long id = preGuiaRepository.saveAndFlush(p).getIdPreGuia();
        historicoRepository.saveAndFlush(new PreGuiaHistorico(
                null, preGuiaRepository.getReferenceById(id), PreGuiaStatus.ATENCAO,
                "Foto ilegivel, envie novamente.", null, setor, LocalDateTime.of(2026, 10, 15, 9, 0)));
        return id;
    }

    private ResultActions reenviar(String login, Long id, Long ocsId, MockMultipartFile arquivo) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/beneficiarios/me/pre-guias/" + id + "/reenvio");
        if (arquivo != null) {
            req.file(arquivo);
        }
        if (ocsId != null) {
            req.param("ocsId", ocsId.toString());
        }
        return mockMvc.perform(req.header(SimulacaoLoginFilter.HEADER, login));
    }

    private static MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("arquivo", nome, "application/octet-stream", conteudo);
    }

    private PreGuia noBanco(Long id) {
        entityManager.flush();
        entityManager.clear();
        return preGuiaRepository.findById(id).orElseThrow();
    }

    private PreGuiaHistorico linhaDoReenvio(Long id) {
        entityManager.flush();
        entityManager.clear();
        List<PreGuiaHistorico> linhas = historicoRepository.findAll().stream()
                .filter(h -> h.getPreGuia().getIdPreGuia().equals(id) && h.getStatus() == PreGuiaStatus.PENDENTE)
                .toList();
        assertThat(linhas).hasSize(1);
        return linhas.get(0);
    }

    // ===== sucesso =====

    @Test
    void soOcs_trocaAOcsMantemOArquivoEVoltaParaPendente() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, ocsNova.getIdOcs(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataReenvio").exists());

        PreGuia p = noBanco(id);
        assertThat(p.getStatus()).isEqualTo(PreGuiaStatus.PENDENTE);
        assertThat(p.getOcs().getIdOcs()).isEqualTo(ocsNova.getIdOcs());
        assertThat(p.getEncaminhamentoUrl()).isEqualTo("encaminhamentos/antigo.jpg");

        PreGuiaHistorico linha = linhaDoReenvio(id);
        assertThat(linha.getUsuario().getIdUsuario()).isEqualTo(beneficiario.getUsuarioId());
        assertThat(linha.getObservacao())
                .contains("OCS: Laboratorio Exemplo -> Hospital Exemplo")
                .contains("Foto ilegivel, envie novamente.");
    }

    @Test
    void soArquivo_oNovoViraOAtualEOAnteriorFicaGuardado() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, null, arquivo("nova-foto.jpg", JPG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        PreGuia p = noBanco(id);
        assertThat(p.getEncaminhamentoUrl()).isNotEqualTo("encaminhamentos/antigo.jpg").endsWith(".jpg");
        assertThat(p.getOcs().getIdOcs()).isEqualTo(ocsAtual.getIdOcs());
        assertThat(armazenamentoArquivo.carregar(p.getEncaminhamentoUrl()).exists()).isTrue();

        PreGuiaHistorico linha = linhaDoReenvio(id);
        assertThat(linha.getEncaminhamentoUrl()).isEqualTo(p.getEncaminhamentoUrl());
        // a foto antiga fica registrada no historico
        assertThat(linha.getObservacao()).contains("Encaminhamento: antigo.jpg -> ");
    }

    @Test
    void osDois_trocaOcsEArquivo() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, ocsNova.getIdOcs(), arquivo("encaminhamento.pdf", PDF))
                .andExpect(status().isOk());

        PreGuia p = noBanco(id);
        assertThat(p.getOcs().getIdOcs()).isEqualTo(ocsNova.getIdOcs());
        assertThat(p.getEncaminhamentoUrl()).endsWith(".pdf");
        assertThat(linhaDoReenvio(id).getObservacao()).contains("Encaminhamento:").contains("OCS:");
    }

    // ===== 422 =====

    @Test
    void nenhum_retorna422SemAlterar() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, null, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("arquivo: Informe uma nova OCS e/ou um novo encaminhamento"));

        assertThat(noBanco(id).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
    }

    @Test
    void arquivoInvalido_retorna422() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, null, arquivo("documento.docx", "texto".getBytes()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("arquivo: Formato invalido. Envie PDF, JPG ou PNG"));
        // extensao certa, conteudo errado
        reenviar(BENEFICIARIO, id, null, arquivo("falso.png", "nao sou png".getBytes()))
                .andExpect(status().isUnprocessableEntity());

        assertThat(noBanco(id).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
    }

    @Test
    void ocsInativaOuInexistente_retorna422() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, ocsInativa.getIdOcs(), null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("ocsId: OCS inexistente ou inativa"));
        reenviar(BENEFICIARIO, id, 999999L, null).andExpect(status().isUnprocessableEntity());
    }

    // ===== 409, 403 e 404 =====

    @Test
    void statusDiferenteDeAtencao_retorna409() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.EM_ANALISE);

        reenviar(BENEFICIARIO, id, ocsNova.getIdOcs(), null).andExpect(status().isConflict());
    }

    @Test
    void reenvioDuplo_oSegundoRetorna409() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, ocsNova.getIdOcs(), null).andExpect(status().isOk());
        reenviar(BENEFICIARIO, id, ocsAtual.getIdOcs(), null).andExpect(status().isConflict());

        assertThat(noBanco(id).getOcs().getIdOcs()).isEqualTo(ocsNova.getIdOcs());
        linhaDoReenvio(id); // uma unica linha de reenvio
    }

    @Test
    void origemMedico_retorna403ComQualquerCombinacao() throws Exception {
        Long id = preGuia(beneficiario, medico, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, ocsNova.getIdOcs(), null).andExpect(status().isForbidden());
        reenviar(BENEFICIARIO, id, null, arquivo("foto.jpg", JPG)).andExpect(status().isForbidden());
        reenviar(BENEFICIARIO, id, ocsNova.getIdOcs(), arquivo("foto.jpg", JPG)).andExpect(status().isForbidden());
        reenviar(BENEFICIARIO, id, null, null).andExpect(status().isForbidden());

        assertThat(noBanco(id).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
    }

    @Test
    void preGuiaDeOutroBeneficiarioOuInexistente_retorna404() throws Exception {
        Long id = preGuia(outroBeneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(BENEFICIARIO, id, ocsNova.getIdOcs(), null).andExpect(status().isNotFound());
        reenviar(BENEFICIARIO, 999999L, ocsNova.getIdOcs(), null).andExpect(status().isNotFound());

        assertThat(noBanco(id).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
    }

    @Test
    void perfilErrado_retorna403() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);

        reenviar(MEDICO, id, ocsNova.getIdOcs(), null).andExpect(status().isForbidden());
    }

    @Test
    void semLogin_retorna401() throws Exception {
        Long id = preGuia(beneficiario, null, PreGuiaStatus.ATENCAO);
        mockMvc.perform(multipart("/beneficiarios/me/pre-guias/" + id + "/reenvio").param("ocsId", ocsNova.getIdOcs().toString()))
                .andExpect(status().isUnauthorized());
    }
}
