package fatec.fusex.nexus.minhasPreGuias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MinhasPreGuiasDependenteTest {

    private static final String LOGIN_TITULAR = "52998224711";
    private static final String LOGIN_RESPONSAVEL = "41282945022";
    private static final String URL = "/beneficiarios/me/pre-guias";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private DependenteRepository dependenteRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private OcsRepository ocsRepository;

    private Beneficiario titular;
    private Ocs ocs;
    private Dependente sofia;

    @BeforeEach
    void cenario() {
        ocs = new Ocs();
        ocs.setNome("Laboratorio Minhas");
        ocs.setTipo(OcsTipo.OCS);
        ocs.setContratoNum("CTR-MINHAS-1");
        ocs = ocsRepository.save(ocs);

        Usuario usuarioTitular = novoUsuario(LOGIN_TITULAR, PerfilUsuario.BENEFICIARIO);
        titular = new Beneficiario();
        titular.setUsuario(usuarioTitular);
        titular.setNome("Antonio Minhas");
        titular.setPrecCp("45872213311");
        titular.setDataNascimento(LocalDate.of(1968, 3, 15));
        titular.setTelefone("12987654321");
        titular = beneficiarioRepository.save(titular);

        Usuario responsavel = novoUsuario(LOGIN_RESPONSAVEL, PerfilUsuario.RESPONSAVEL_EXTERNO);
        sofia = new Dependente();
        sofia.setTitular(titular);
        sofia.setResponsavel(responsavel);
        sofia.setNome("Sofia Minhas");
        sofia.setDataNascimento(LocalDate.of(2017, 5, 15));
        sofia.setCpf("39053344999");
        sofia.setPrecCp("12345678999");
        sofia.setTipo(TipoDependente.MENOR_RESPONSAVEL);
        sofia.setVigenciaFim(LocalDate.of(2039, 5, 14));
        sofia = dependenteRepository.save(sofia);

        PreGuia daSofia = new PreGuia();
        daSofia.setBeneficiario(titular);
        daSofia.setDependente(sofia);
        daSofia.setCriadaPor(responsavel);
        daSofia.setOcs(ocs);
        daSofia.setEncaminhamentoUrl("encaminhamento.pdf");
        daSofia.setDataCriacao(LocalDateTime.now());
        preGuiaRepository.save(daSofia);
    }

    @Test
    void responsavelVeAPreGuiaDoMenorQueAcompanha() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void titularNaoVeAPreGuiaDoMenorSobResponsavel() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }
}
