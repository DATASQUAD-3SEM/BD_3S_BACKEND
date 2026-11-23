package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T30-BE2 (SCRUM-259): GET /pre-guias/pacientes e POST /pre-guias do dependente maior sem transacao de teste
 * envolvendo a requisicao (como na aplicacao real, com open-in-view desligado). Grava no H2 de verdade; o
 * @AfterEach apaga tudo o que o teste criou.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreGuiaPacientesIntegracaoTest {

    private static final String LOGIN_TITULAR = "74100000001";
    private static final String LOGIN_MAIOR = "74100000002";
    private static final String PREC_CP_TITULAR = "74100000022";
    private static final String PREC_CP_MENOR = "74100000033";
    private static final String PREC_CP_MAIOR = "74100000044";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BeneficiarioRepository beneficiarioRepository;
    @Autowired
    private DependenteRepository dependenteRepository;
    @Autowired
    private OcsRepository ocsRepository;
    @Autowired
    private PreGuiaRepository preGuiaRepository;
    @Autowired
    private PreGuiaHistoricoRepository historicoRepository;

    private Long titularId;
    private Long menorId;
    private Long maiorId;
    private Long ocsId;

    @BeforeEach
    void dados() {
        Beneficiario titular = new Beneficiario();
        titular.setUsuario(usuario(LOGIN_TITULAR));
        titular.setNome("Titular Pacientes");
        titular.setPrecCp(PREC_CP_TITULAR);
        titular.setDataNascimento(LocalDate.of(1968, 3, 15));
        titular.setTelefone("12987654321");
        titular = beneficiarioRepository.saveAndFlush(titular);
        titularId = titular.getUsuarioId();

        menorId = dependente(titular, null, "Menor Pacientes", LocalDate.of(2020, 3, 21), "74100000055",
                PREC_CP_MENOR, TipoDependente.MENOR_TITULAR, LocalDate.of(2042, 3, 20)).getIdDependente();
        maiorId = dependente(titular, usuario(LOGIN_MAIOR), "Maior Pacientes", LocalDate.of(1990, 2, 8),
                LOGIN_MAIOR, PREC_CP_MAIOR, TipoDependente.MAIOR, null).getIdDependente();

        Ocs ocs = new Ocs();
        ocs.setNome("OCS Pacientes Integracao");
        ocs.setContratoNum("PAC-INT-1");
        ocs.setTipo(OcsTipo.OCS);
        ocsId = ocsRepository.saveAndFlush(ocs).getIdOcs();
    }

    @AfterEach
    void limpar() {
        preGuiaRepository.findAll().stream()
                .filter(p -> p.getOcs().getIdOcs().equals(ocsId))
                .forEach(p -> {
                    historicoRepository.findAll().stream()
                            .filter(h -> h.getPreGuia().getIdPreGuia().equals(p.getIdPreGuia()))
                            .forEach(historicoRepository::delete);
                    preGuiaRepository.delete(p);
                });
        ocsRepository.deleteById(ocsId);
        dependenteRepository.deleteById(menorId);
        dependenteRepository.deleteById(maiorId);
        beneficiarioRepository.deleteById(titularId);
        usuarioRepository.findByLogin(LOGIN_MAIOR).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(LOGIN_TITULAR).ifPresent(usuarioRepository::delete);
    }

    @Test
    void titular_recebeOsDadosDeleEDoMenor() throws Exception {
        mockMvc.perform(get("/pre-guias/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].precCp").value(PREC_CP_TITULAR))
                .andExpect(jsonPath("$[0].cpf").value(LOGIN_TITULAR))
                .andExpect(jsonPath("$[1].dependenteId").value(menorId))
                .andExpect(jsonPath("$[1].precCp").value(PREC_CP_MENOR));
    }

    @Test
    void maior_recebeOsPropriosDadosECriaAPreGuiaSemDependenteId() throws Exception {
        mockMvc.perform(get("/pre-guias/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_MAIOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dependenteId").value(maiorId))
                .andExpect(jsonPath("$[0].precCp").value(PREC_CP_MAIOR));

        mockMvc.perform(multipart("/pre-guias")
                        .file(new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf",
                                "%PDF-1.4 conteudo".getBytes()))
                        .param("ocsId", String.valueOf(ocsId))
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_MAIOR))
                .andExpect(status().isCreated());

        assertThat(preGuiaRepository.findAll().stream()
                .filter(p -> p.getOcs().getIdOcs().equals(ocsId))
                .filter(p -> p.getDependente() != null && p.getDependente().getIdDependente().equals(maiorId))
                .count()).isEqualTo(1);
    }

    private Dependente dependente(Beneficiario titular, Usuario conta, String nome, LocalDate nascimento, String cpf,
                                  String precCp, TipoDependente tipo, LocalDate vigenciaFim) {
        Dependente d = new Dependente();
        d.setTitular(titular);
        d.setUsuario(conta);
        d.setNome(nome);
        d.setDataNascimento(nascimento);
        d.setCpf(cpf);
        d.setPrecCp(precCp);
        d.setTipo(tipo);
        d.setVigenciaFim(vigenciaFim);
        return dependenteRepository.saveAndFlush(d);
    }

    private Usuario usuario(String login) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
