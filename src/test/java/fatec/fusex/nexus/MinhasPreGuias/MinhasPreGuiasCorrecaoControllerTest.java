package fatec.fusex.nexus.MinhasPreGuias;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class MinhasPreGuiasCorrecaoControllerTest {

    private static final String URL = "/beneficiarios/me/pre-guias/{id}/correcao";
    private static final String HEADER = SimulacaoLoginFilter.HEADER;
    private static final String LOGIN_JOAO = "11111111111";
    private static final String LOGIN_MARIA = "22222222222";
    private static final String LOGIN_SETOR = "setor.guias";

    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 10, 10, 9, 0);
    private static final LocalDateTime OBSERVACAO = LocalDateTime.of(2026, 10, 11, 15, 30);
    private static final LocalDateTime REENVIO = LocalDateTime.of(2026, 10, 12, 14, 30);

    private final MockMvc mockMvc;
    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoFusexRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    private Usuario usuarioJoao;
    private Beneficiario joao;
    private Beneficiario maria;
    private Ocs ocsPadrao;
    private MedicoFusex medico;

    MinhasPreGuiasCorrecaoControllerTest(
            MockMvc mockMvc,
            PreGuiaRepository preGuiaRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            UsuarioRepository usuarioRepository,
            MedicoFusexRepository medicoFusexRepository,
            PreGuiaHistoricoRepository historicoRepository) {
        this.mockMvc = mockMvc;
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.usuarioRepository = usuarioRepository;
        this.medicoFusexRepository = medicoFusexRepository;
        this.historicoRepository = historicoRepository;
    }

    @BeforeEach
    void setUp() {
        usuarioJoao = criarUsuario(LOGIN_JOAO, PerfilUsuario.BENEFICIARIO);
        joao = criarBeneficiario(usuarioJoao, "João Logado", LOGIN_JOAO);

        Usuario usuarioMaria = criarUsuario(LOGIN_MARIA, PerfilUsuario.BENEFICIARIO);
        maria = criarBeneficiario(usuarioMaria, "Maria Outra", LOGIN_MARIA);

        criarUsuario(LOGIN_SETOR, PerfilUsuario.SETOR_GUIAS);

        Usuario usuarioMedico = criarUsuario("33333333333", PerfilUsuario.MEDICO);
        medico = new MedicoFusex();
        medico.setUsuario(usuarioMedico);
        medico.setNome("Dr. Carlos");
        medico.setPrecCp("33333333333");
        medico.setPatente("Capitão");
        medico.setCrm("123456-SP");
        medico = medicoFusexRepository.save(medico);

        ocsPadrao = new Ocs();
        ocsPadrao.setNome("Hospital Central");
        ocsPadrao.setContratoNum("123456789");
        ocsPadrao.setTipo(OcsTipo.OCS);
        ocsPadrao = ocsRepository.save(ocsPadrao);
    }

    @Test
    @DisplayName("sucesso: devolve dados da correcao de pre-guia em Atencao")
    void sucesso_retornaDadosDaCorrecao() throws Exception {
        PreGuia pg = criarPreGuia(joao, PreGuiaStatus.ATENCAO, null, "encaminhamentos/original.jpg");
        criarHistorico(pg, PreGuiaStatus.ATENCAO, "Foto desfocada",
                "encaminhamentos/original.jpg", OBSERVACAO);

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(numero(pg)))
                .andExpect(jsonPath("$.status").value("ATENCAO"))
                .andExpect(jsonPath("$.beneficiario.nome").value("João Logado"))
                .andExpect(jsonPath("$.beneficiario.precCp").value(LOGIN_JOAO))
                .andExpect(jsonPath("$.dataCriacao").value("2026-10-10T09:00:00"))
                .andExpect(jsonPath("$.observacao.texto").value("Foto desfocada"))
                .andExpect(jsonPath("$.observacao.dataRegistro").value("2026-10-11T15:30:00"))
                .andExpect(jsonPath("$.encaminhamentoAtual.nome").value("original.jpg"))
                .andExpect(jsonPath("$.encaminhamentoAtual.tipo").value("JPG"))
                .andExpect(jsonPath("$.encaminhamentoAtual.dataEnvio").value("2026-10-10T09:00:00"))
                .andExpect(jsonPath("$.ocsAtual.id").value(ocsPadrao.getIdOcs().intValue()))
                .andExpect(jsonPath("$.ocsAtual.nome").value("Hospital Central"));
    }

    @Test
    @DisplayName("dataEnvio vem do reenvio, nao da observacao do setor")
    void dataEnvio_usaReenvioNaoObservacao() throws Exception {
        PreGuia pg = criarPreGuia(joao, PreGuiaStatus.ATENCAO, null, "encaminhamentos/nova.png");
        criarHistorico(pg, PreGuiaStatus.PENDENTE, null, "encaminhamentos/nova.png", REENVIO);
        criarHistorico(pg, PreGuiaStatus.ATENCAO, "Documento do medico ilegivel",
                "encaminhamentos/nova.png", OBSERVACAO.plusDays(1));

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaminhamentoAtual.tipo").value("PNG"))
                .andExpect(jsonPath("$.encaminhamentoAtual.dataEnvio").value("2026-10-12T14:30:00"))
                .andExpect(jsonPath("$.observacao.texto").value("Documento do medico ilegivel"));
    }

    @Test
    @DisplayName("409: pre-guia fora de Atencao")
    void statusDiferenteDeAtencao_retorna409() throws Exception {
        PreGuia pg = criarPreGuia(joao, PreGuiaStatus.PENDENTE, null, "encaminhamentos/original.jpg");

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("404: pre-guia de outro beneficiario")
    void preGuiaDeOutroBeneficiario_retorna404() throws Exception {
        PreGuia pg = criarPreGuia(maria, PreGuiaStatus.ATENCAO, null, "encaminhamentos/original.jpg");

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404: pre-guia inexistente")
    void preGuiaInexistente_retorna404() throws Exception {
        mockMvc.perform(get(URL, 99999L).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("403: pre-guia criada por medico")
    void origemMedico_retorna403() throws Exception {
        PreGuia pg = criarPreGuia(joao, PreGuiaStatus.ATENCAO, medico, "encaminhamentos/original.jpg");

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403: perfil diferente de BENEFICIARIO")
    void perfilErrado_retorna403() throws Exception {
        PreGuia pg = criarPreGuia(joao, PreGuiaStatus.ATENCAO, null, "encaminhamentos/original.jpg");

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_SETOR))
                .andExpect(status().isForbidden());
    }

    // ===== helpers =====

    private Usuario criarUsuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(perfil);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    private Beneficiario criarBeneficiario(Usuario usuario, String nome, String precCp) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1990, 1, 1));
        b.setTelefone("11999999991");
        return beneficiarioRepository.save(b);
    }

    private PreGuia criarPreGuia(Beneficiario dono, PreGuiaStatus status,
                                 MedicoFusex medicoCriador, String encaminhamentoUrl) {
        PreGuia pg = new PreGuia();
        pg.setBeneficiario(dono);
        pg.setOcs(ocsPadrao);
        pg.setStatus(status);
        pg.setDataCriacao(CRIACAO);
        pg.setEncaminhamentoUrl(encaminhamentoUrl);
        pg.setMedicoFusex(medicoCriador);
        return preGuiaRepository.save(pg);
    }

    private void criarHistorico(PreGuia pg, PreGuiaStatus status, String observacao,
                                String encaminhamentoUrl, LocalDateTime dataHora) {
        PreGuiaHistorico h = new PreGuiaHistorico(
                null, pg, status, observacao, encaminhamentoUrl, usuarioJoao, dataHora);
        historicoRepository.save(h);
    }

    private static String numero(PreGuia pg) {
        return String.format("%04d", pg.getIdPreGuia());
    }
}