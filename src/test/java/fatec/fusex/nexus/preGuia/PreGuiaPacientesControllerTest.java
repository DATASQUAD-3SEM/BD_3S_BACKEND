package fatec.fusex.nexus.preGuia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PreGuiaPacientesControllerTest {

    private static final String LOGIN_TITULAR = "52998224700";
    private static final String LOGIN_RESPONSAVEL = "41282945067";
    private static final String LOGIN_MAIOR = "98765432100";
    private static final String BASE = "/pre-guias";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private DependenteRepository dependenteRepository;
    @Autowired private fatec.fusex.nexus.ocs.OcsRepository ocsRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;

    private Dependente miguel;
    private Dependente sofia;
    private Dependente carla;

    @BeforeEach
    void cenario() {
        Usuario titular = novoUsuario(LOGIN_TITULAR, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(titular);
        beneficiario.setNome("Antonio Titular");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.save(beneficiario);

        Usuario responsavel = novoUsuario(LOGIN_RESPONSAVEL, PerfilUsuario.RESPONSAVEL_EXTERNO);
        Usuario maior = novoUsuario(LOGIN_MAIOR, PerfilUsuario.BENEFICIARIO);

        miguel = dependente(beneficiario, null, null, "Miguel Ferreira", LocalDate.of(2026, 3, 21),
                "39053344705", "12345678902", TipoDependente.MENOR_TITULAR, LocalDate.of(2048, 3, 20));
        sofia = dependente(beneficiario, null, responsavel, "Sofia Ferreira", LocalDate.of(2017, 5, 15),
                "39053344806", "12345678903", TipoDependente.MENOR_RESPONSAVEL, LocalDate.of(2039, 5, 14));
        carla = dependente(beneficiario, maior, null, "Carla Ferreira", LocalDate.of(1985, 2, 8),
                "98765432100", "12345678901", TipoDependente.MAIOR, null);
    }

    @Test
    void titularListaSiMesmoEMenoresSobSuaGuarda() throws Exception {
        mockMvc.perform(get(BASE + "/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dependenteId").doesNotExist())
                .andExpect(jsonPath("$[1].nome").value("Miguel Ferreira"));
    }

    @Test
    void responsavelExternoListaSoOsMenoresQueAcompanha() throws Exception {
        mockMvc.perform(get(BASE + "/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Sofia Ferreira"));
    }

    @Test
    void maiorListaSoASiMesmo() throws Exception {
        mockMvc.perform(get(BASE + "/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_MAIOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Carla Ferreira"))
                .andExpect(jsonPath("$[0].relacao").value("Você (dependente maior)"));
    }

    @Test
    void titularRecebeOsPropriosDadosParaOBlocoDadosDoPaciente() throws Exception {
        mockMvc.perform(get(BASE + "/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Antonio Titular"))
                .andExpect(jsonPath("$[0].dataNascimento").value("1968-03-15"))
                .andExpect(jsonPath("$[0].idade").isNumber())
                .andExpect(jsonPath("$[0].faixaEtaria").isString())
                .andExpect(jsonPath("$[0].precCp").value("45872213300"))
                .andExpect(jsonPath("$[0].cpf").value(LOGIN_TITULAR))
                .andExpect(jsonPath("$[0].telefone").value("12987654321"))
                .andExpect(jsonPath("$[1].precCp").value("12345678902"))
                .andExpect(jsonPath("$[1].cpf").value("39053344705"))
                .andExpect(jsonPath("$[1].dataNascimento").value("2026-03-21"));
    }

    @Test
    void responsavelRecebeOsDadosDoMenorQueAcompanha() throws Exception {
        mockMvc.perform(get(BASE + "/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relacao").value("Menor que você acompanha como responsável legal"))
                .andExpect(jsonPath("$[0].precCp").value("12345678903"))
                .andExpect(jsonPath("$[0].cpf").value("39053344806"))
                .andExpect(jsonPath("$[0].telefone").doesNotExist());
    }

    @Test
    void maiorRecebeOsPropriosDados() throws Exception {
        mockMvc.perform(get(BASE + "/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_MAIOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dependenteId").value(carla.getIdDependente()))
                .andExpect(jsonPath("$[0].precCp").value("12345678901"))
                .andExpect(jsonPath("$[0].cpf").value("98765432100"));
    }

    @Test
    void maiorCriaAPropriaPreGuiaComESemDependenteId() throws Exception {
        Long ocsId = ocsAtiva();

        String semId = mockMvc.perform(multipart(BASE).file(encaminhamento())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_MAIOR)
                        .param("ocsId", String.valueOf(ocsId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String comId = mockMvc.perform(multipart(BASE).file(encaminhamento())
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_MAIOR)
                        .param("ocsId", String.valueOf(ocsId))
                        .param("dependenteId", String.valueOf(carla.getIdDependente())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        for (String resposta : new String[] {semId, comId}) {
            Long id = Long.valueOf(resposta.replaceAll(".*\"id\":(\\d+).*", "$1"));
            PreGuia criada = preGuiaRepository.findById(id).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(criada.getDependente().getIdDependente())
                    .isEqualTo(carla.getIdDependente());
            org.assertj.core.api.Assertions.assertThat(criada.getBeneficiario().getNome()).isEqualTo("Antonio Titular");
        }
    }

    private Long ocsAtiva() {
        fatec.fusex.nexus.ocs.Ocs ocs = new fatec.fusex.nexus.ocs.Ocs();
        ocs.setNome("OCS Pacientes");
        ocs.setContratoNum("PAC-001");
        ocs.setTipo(fatec.fusex.nexus.ocs.OcsTipo.OCS);
        ocs.setAtivo(true);
        return ocsRepository.save(ocs).getIdOcs();
    }

    private static org.springframework.mock.web.MockMultipartFile encaminhamento() {
        return new org.springframework.mock.web.MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf",
                "%PDF-1.4 conteudo".getBytes());
    }

    @Test
    void responsavelSemDependenteIdRecebe422() throws Exception {
        mockMvc.perform(multipart(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL)
                        .param("ocsId", "1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void criarParaDependenteNaoPermitidoRecebe403() throws Exception {
        mockMvc.perform(multipart(BASE).header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL)
                        .param("ocsId", "1")
                        .param("dependenteId", String.valueOf(miguel.getIdDependente())))
                .andExpect(status().isForbidden());
    }

    private Dependente dependente(Beneficiario titular, Usuario conta, Usuario responsavel, String nome,
                                  LocalDate nascimento, String cpf, String precCp, TipoDependente tipo,
                                  LocalDate vigenciaFim) {
        Dependente d = new Dependente();
        d.setTitular(titular);
        d.setUsuario(conta);
        d.setResponsavel(responsavel);
        d.setNome(nome);
        d.setDataNascimento(nascimento);
        d.setCpf(cpf);
        d.setPrecCp(precCp);
        d.setTipo(tipo);
        d.setVigenciaFim(vigenciaFim);
        return dependenteRepository.save(d);
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }
}
