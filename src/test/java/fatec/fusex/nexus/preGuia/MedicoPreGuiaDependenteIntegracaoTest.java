package fatec.fusex.nexus.preGuia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRolAns;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T30-BE7 (SCRUM-264): GET /medicos/beneficiarios?precCp= de dependente e POST /medicos/pre-guias com dependenteId,
 * sem transacao de teste envolvendo a requisicao (como na aplicacao real, com open-in-view desligado); grava no H2 de
 * verdade, como o endpoint real.
 * O @AfterEach apaga tudo o que o teste criou.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedicoPreGuiaDependenteIntegracaoTest {

    private static final String LOGIN_MEDICO = "71000000001";
    private static final String LOGIN_TITULAR = "71000000002";
    private static final String CPF_DEPENDENTE = "71000000003";
    private static final String PREC_CP_TITULAR = "71000000022";
    private static final String PREC_CP_DEPENDENTE = "71000000033";
    private static final String PREC_CP_VENCIDO = "71000000044";
    private static final String COD_TUSS = "71000001";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private MedicoFusexRepository medicoRepository;
    @Autowired
    private BeneficiarioRepository beneficiarioRepository;
    @Autowired
    private DependenteRepository dependenteRepository;
    @Autowired
    private OcsRepository ocsRepository;
    @Autowired
    private ProcedimentoExameRepository procedimentoRepository;
    @Autowired
    private PreGuiaRepository preGuiaRepository;
    @Autowired
    private PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    @Autowired
    private PreGuiaHistoricoRepository historicoRepository;

    private Long ocsId;
    private Long titularId;
    private Long dependenteVigenteId;
    private Long dependenteVencidoId;

    @BeforeEach
    void dados() {
        Usuario medicoUsuario = usuario(LOGIN_MEDICO, PerfilUsuario.MEDICO);
        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(medicoUsuario);
        medico.setNome("Medico Dependente");
        medico.setPatente("Capitao");
        medico.setCrm("CRM-DEP-1");
        medico.setPrecCp("71000000011");
        medicoRepository.saveAndFlush(medico);

        Usuario titularUsuario = usuario(LOGIN_TITULAR, PerfilUsuario.BENEFICIARIO);
        Beneficiario titular = new Beneficiario();
        titular.setUsuario(titularUsuario);
        titular.setNome("Titular Dependente");
        titular.setPrecCp(PREC_CP_TITULAR);
        titular.setDataNascimento(LocalDate.of(1970, 1, 1));
        titular.setTelefone("12999990001");
        titularId = beneficiarioRepository.saveAndFlush(titular).getUsuarioId();

        dependenteVigenteId = dependente(titular, PREC_CP_DEPENDENTE, LocalDate.now().plusYears(2)).getIdDependente();
        dependenteVencidoId = dependente(titular, PREC_CP_VENCIDO, LocalDate.now().minusDays(1)).getIdDependente();

        Ocs ocs = new Ocs();
        ocs.setNome("OCS Dependente");
        ocs.setContratoNum("DEP-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsId = ocsRepository.saveAndFlush(ocs).getIdOcs();

        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss(COD_TUSS);
        p.setTerminologiaProcedimento("Procedimento Dependente");
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        procedimentoRepository.saveAndFlush(p);
    }

    @AfterEach
    void limpar() {
        historicoRepository.deleteAll();
        preGuiaProcedimentoRepository.deleteAll();
        preGuiaRepository.deleteAll();
        procedimentoRepository.findByCodigoTussIn(List.of(COD_TUSS)).forEach(procedimentoRepository::delete);
        ocsRepository.deleteById(ocsId);
        dependenteRepository.findAll().stream()
                .filter(d -> List.of(PREC_CP_DEPENDENTE, PREC_CP_VENCIDO).contains(d.getPrecCp()))
                .forEach(dependenteRepository::delete);
        beneficiarioRepository.findById(titularId).ifPresent(beneficiarioRepository::delete);
        medicoRepository.findByUsuarioLogin(LOGIN_MEDICO).ifPresent(medicoRepository::delete);
        usuarioRepository.findByLogin(LOGIN_TITULAR).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(LOGIN_MEDICO).ifPresent(usuarioRepository::delete);
    }

    @Test
    void buscaPorPrecCpDeDependenteVigente_devolveOBlocoDoDependente() throws Exception {
        mockMvc.perform(get("/medicos/beneficiarios").param("precCp", PREC_CP_DEPENDENTE)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_MEDICO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_DEPENDENTE))
                .andExpect(jsonPath("$.dependente.titularNome").value("Titular Dependente"))
                .andExpect(jsonPath("$.dependente.titularPrecCp").value(PREC_CP_TITULAR))
                .andExpect(jsonPath("$.dependente.menor").value(true))
                .andExpect(jsonPath("$.dependente.responsavel").value("Titular Dependente"));
    }

    @Test
    void buscaPorPrecCpDeDependenteComVigenciaVencida_retorna404() throws Exception {
        mockMvc.perform(get("/medicos/beneficiarios").param("precCp", PREC_CP_VENCIDO)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_MEDICO))
                .andExpect(status().isNotFound());
    }

    @Test
    void criaPreGuiaDoDependenteVigente() throws Exception {


        mockMvc.perform(post("/medicos/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_MEDICO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiarioId\": " + titularId + ", \"ocsId\": " + ocsId
                                + ", \"procedimentos\": [\"" + COD_TUSS + "\"], \"dependenteId\": " + dependenteVigenteId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        org.assertj.core.api.Assertions.assertThat(
                preGuiaRepository.findAll().stream().filter(p -> p.getDependente() != null).count()).isEqualTo(1);
    }

    @Test
    void recusaDependenteComVigenciaVencida() throws Exception {


        mockMvc.perform(post("/medicos/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_MEDICO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiarioId\": " + titularId + ", \"ocsId\": " + ocsId
                                + ", \"procedimentos\": [\"" + COD_TUSS + "\"], \"dependenteId\": " + dependenteVencidoId + "}"))
                .andExpect(status().isNotFound());

        org.assertj.core.api.Assertions.assertThat(preGuiaRepository.count()).isZero();
    }

    private Dependente dependente(Beneficiario titular, String precCp, LocalDate vigenciaFim) {
        Dependente d = new Dependente();
        d.setTitular(titular);
        d.setNome("Dependente " + precCp);
        d.setDataNascimento(LocalDate.now().minusYears(8));
        d.setCpf(precCp.equals(PREC_CP_DEPENDENTE) ? CPF_DEPENDENTE : "71000000055");
        d.setPrecCp(precCp);
        d.setVinculo("Filho");
        d.setTelefone("12999990002");
        d.setTipo(TipoDependente.MENOR_TITULAR);
        d.setVigenciaFim(vigenciaFim);
        return dependenteRepository.saveAndFlush(d);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
