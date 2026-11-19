package fatec.fusex.nexus.dependente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/**
 * T30-BE2 de ponta a ponta (MockMvc + H2): GET /pre-guias/pacientes e POST /pre-guias com dependenteId.
 * Cenario: Antonio (titular) com Lucas (menor, so ele responsavel), Pedro (maior), Ana (encerrada), Helena
 * (maior com conta propria, tambem responsavel de Lucas); Marta (titular) com Sofia (menor), que tem Renata
 * (responsavel externo) como segunda responsavel.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PacientesPreGuiaIntegracaoTest {

    private static final LocalDate HOJE = LocalDate.now(PacientesPreGuiaService.FUSO);

    private static final String ANTONIO = "52998224725";
    private static final String MARTA = "70000000663";
    private static final String HELENA = "30000000205";
    private static final String RENATA = "60000000574";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private DependenteRepository dependenteRepository;
    @Autowired private ResponsavelLegalRepository responsavelLegalRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private EntityManager entityManager;

    private Ocs ocs;
    private Dependente lucas;
    private Dependente pedro;
    private Dependente ana;
    private Dependente helena;
    private Dependente sofia;

    private final MockMultipartFile arquivo =
            new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf", "%PDF-1.4 conteudo".getBytes());

    @BeforeEach
    void cenario() {
        Usuario uAntonio = usuario(ANTONIO, PerfilUsuario.BENEFICIARIO);
        Beneficiario antonio = beneficiario(uAntonio, "Antonio Carlos Ferreira", "45872213300");
        Usuario uMarta = usuario(MARTA, PerfilUsuario.BENEFICIARIO);
        Beneficiario marta = beneficiario(uMarta, "Marta Souza Lima", "45810020001");
        Usuario uHelena = usuario(HELENA, PerfilUsuario.BENEFICIARIO);
        Usuario uRenata = usuario(RENATA, PerfilUsuario.RESPONSAVEL_EXTERNO);

        ocs = new Ocs();
        ocs.setNome("Hospital Exemplo");
        ocs.setContratoNum("EXEMPLO-001");
        ocs.setTipo(OcsTipo.OCS);
        ocs.setAtivo(true);
        ocsRepository.save(ocs);

        lucas = dependente(antonio, null, "Lucas Ferreira", HOJE.minusYears(10), "45872213301", "Filho",
                TipoVigencia.DETERMINADA, HOJE.plusYears(5), SituacaoDependente.ATIVO, null);
        pedro = dependente(antonio, null, "Pedro Ferreira", HOJE.minusYears(30), "45872213304", "Irmao",
                TipoVigencia.DETERMINADA, HOJE.plusYears(5), SituacaoDependente.ATIVO, null);
        helena = dependente(antonio, uHelena, "Helena Ferreira", HOJE.minusYears(50), "45872213303", "Conjuge",
                TipoVigencia.VITALICIA, null, SituacaoDependente.ATIVO, null);
        ana = dependente(antonio, null, "Ana Ferreira", HOJE.minusYears(9), "45872213399", "Filha",
                TipoVigencia.VITALICIA, null, SituacaoDependente.ENCERRADO, LocalDateTime.now());
        sofia = dependente(marta, null, "Sofia Souza", HOJE.minusYears(8), "45810020002", "Filha",
                TipoVigencia.DETERMINADA, HOJE.plusYears(5), SituacaoDependente.ATIVO, null);

        responsavel(lucas, uAntonio, HOJE.minusYears(10), null);
        responsavel(lucas, uHelena, HOJE.minusYears(4), null); // acumulo de papeis: Helena e dependente e responsavel
        responsavel(ana, uAntonio, HOJE.minusYears(9), null);
        responsavel(sofia, uMarta, HOJE.minusYears(8), null);
        responsavel(sofia, uRenata, HOJE.minusYears(8), null);
        entityManager.flush();
    }

    @Test
    void listar_titularVeOProprioEMenoresQueAcompanha() throws Exception {
        pacientes(ANTONIO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dependenteId").doesNotExist())
                .andExpect(jsonPath("$[0].nome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$[0].relacao").value("Titular"))
                .andExpect(jsonPath("$[1].dependenteId").value(lucas.getIdDependente()))
                .andExpect(jsonPath("$[1].nome").value("Lucas Ferreira"))
                .andExpect(jsonPath("$[1].relacao").value("Filho"));
    }

    @Test
    void listar_responsavelExternoSoVeOsMenoresQueAcompanha() throws Exception {
        pacientes(RENATA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dependenteId").value(sofia.getIdDependente()))
                .andExpect(jsonPath("$[0].nome").value("Sofia Souza"));
    }

    @Test
    void listar_dependenteMaiorVeELeEOsMenoresQueAcompanha() throws Exception {
        pacientes(HELENA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dependenteId").value(helena.getIdDependente()))
                .andExpect(jsonPath("$[0].relacao").value("Conjuge"))
                .andExpect(jsonPath("$[1].dependenteId").value(lucas.getIdDependente()));
    }

    @Test
    void criar_titularComMenorQueAcompanha_gravaPacienteMenorECriador() throws Exception {
        criar(ANTONIO, lucas.getIdDependente()).andExpect(status().isCreated());

        PreGuia preGuia = unicaPreGuia();
        assertThat(preGuia.getDependente().getIdDependente()).isEqualTo(lucas.getIdDependente());
        assertThat(preGuia.getBeneficiario().getUsuarioId()).isEqualTo(antonioId());
        assertThat(preGuia.getPacienteMenorNaCriacao()).isTrue();
        assertThat(preGuia.getCriadaPor().getLogin()).isEqualTo(ANTONIO);
    }

    @Test
    void criar_responsavelExternoComMenorQueAcompanha_gravaOTitularDoMenor() throws Exception {
        criar(RENATA, sofia.getIdDependente()).andExpect(status().isCreated());

        PreGuia preGuia = unicaPreGuia();
        assertThat(preGuia.getDependente().getIdDependente()).isEqualTo(sofia.getIdDependente());
        assertThat(preGuia.getBeneficiario().getUsuarioId()).isEqualTo(martaId());
        assertThat(preGuia.getCriadaPor().getLogin()).isEqualTo(RENATA);
    }

    @Test
    void criar_responsavelExternoSemDependenteId_devolve422NoCampo() throws Exception {
        criar(RENATA, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(containsString("dependenteId")));
        assertThat(preGuiaRepository.findAll()).isEmpty();
    }

    @Test
    void criar_menorQueOUsuarioNaoAcompanha_devolve403() throws Exception {
        criar(RENATA, lucas.getIdDependente()).andExpect(status().isForbidden());
        assertThat(preGuiaRepository.findAll()).isEmpty();
    }

    @Test
    void criar_dependenteEncerrado_devolve403() throws Exception {
        criar(ANTONIO, ana.getIdDependente()).andExpect(status().isForbidden());
        assertThat(preGuiaRepository.findAll()).isEmpty();
    }

    @Test
    void criar_dependenteMaiorDeIdade_devolve403ParaOTitular() throws Exception {
        criar(ANTONIO, pedro.getIdDependente()).andExpect(status().isForbidden());
        assertThat(preGuiaRepository.findAll()).isEmpty();
    }

    @Test
    void criar_dependenteMaiorCriaParaSiMesmo_pacienteNaoEMenor() throws Exception {
        criar(HELENA, helena.getIdDependente()).andExpect(status().isCreated());

        PreGuia preGuia = unicaPreGuia();
        assertThat(preGuia.getDependente().getIdDependente()).isEqualTo(helena.getIdDependente());
        assertThat(preGuia.getPacienteMenorNaCriacao()).isFalse();
        assertThat(preGuia.getCriadaPor().getLogin()).isEqualTo(HELENA);
    }

    @Test
    void criar_titularSemDependenteId_seguiComoAntes() throws Exception {
        criar(ANTONIO, null).andExpect(status().isCreated());

        PreGuia preGuia = unicaPreGuia();
        assertThat(preGuia.getDependente()).isNull();
        assertThat(preGuia.getPacienteMenorNaCriacao()).isFalse();
        assertThat(preGuia.getBeneficiario().getUsuarioId()).isEqualTo(antonioId());
    }

    // ---------------------------------------------------------------- auxiliares

    private ResultActions pacientes(String login) throws Exception {
        return mockMvc.perform(get("/pre-guias/pacientes").header(SimulacaoLoginFilter.HEADER, login));
    }

    private ResultActions criar(String login, Long dependenteId) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/pre-guias").file(arquivo)
                .param("ocsId", ocs.getIdOcs().toString());
        if (dependenteId != null) {
            req.param("dependenteId", dependenteId.toString());
        }
        return mockMvc.perform(req.header(SimulacaoLoginFilter.HEADER, login));
    }

    private PreGuia unicaPreGuia() {
        entityManager.flush();
        List<PreGuia> todas = preGuiaRepository.findAll();
        assertThat(todas).hasSize(1);
        return todas.get(0);
    }

    private Long antonioId() {
        return usuarioRepository.findByLogin(ANTONIO).orElseThrow().getIdUsuario();
    }

    private Long martaId() {
        return usuarioRepository.findByLogin(MARTA).orElseThrow().getIdUsuario();
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    private Beneficiario beneficiario(Usuario usuario, String nome, String precCp) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1970, 1, 1));
        b.setTelefone("12987654321");
        return beneficiarioRepository.save(b);
    }

    private Dependente dependente(Beneficiario titular, Usuario usuario, String nome, LocalDate nascimento,
                                  String precCp, String vinculo, TipoVigencia tipo, LocalDate vigenciaFim,
                                  SituacaoDependente situacao, LocalDateTime encerramento) {
        Dependente d = new Dependente();
        d.setTitular(titular);
        d.setUsuario(usuario);
        d.setNome(nome);
        d.setDataNascimento(nascimento);
        d.setCpf("12345678909");
        d.setPrecCp(precCp);
        d.setVinculo(vinculo);
        d.setTipoVigencia(tipo);
        d.setVigenciaFim(vigenciaFim);
        d.setSituacao(situacao);
        d.setDataEncerramento(encerramento);
        return dependenteRepository.save(d);
    }

    private void responsavel(Dependente dependente, Usuario usuario, LocalDate inicio, LocalDate fim) {
        ResponsavelLegal r = new ResponsavelLegal();
        r.setDependente(dependente);
        r.setUsuario(usuario);
        r.setDataInicio(inicio);
        r.setDataFim(fim);
        responsavelLegalRepository.save(r);
    }
}
