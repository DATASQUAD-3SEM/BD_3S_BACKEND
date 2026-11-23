package fatec.fusex.nexus.preGuia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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

/** T30-BE6 (SCRUM-263): vigencia vencida so tira o dependente de novas pre-guias; nao altera Guias existentes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VigenciaVencidaTest {

    private static final String LOGIN_TITULAR = "52998224733";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private DependenteRepository dependenteRepository;

    @Test
    void dependenteComVigenciaVencidaSaiDaListaDeNovasPreGuias() throws Exception {
        Beneficiario titular = titular();
        Dependente expirado = new Dependente();
        expirado.setTitular(titular);
        expirado.setNome("Pedro Expirado");
        expirado.setDataNascimento(LocalDate.of(2012, 1, 1));
        expirado.setCpf("39053344888");
        expirado.setPrecCp("12345678888");
        expirado.setTipo(TipoDependente.MENOR_TITULAR);
        expirado.setVigenciaFim(LocalDate.now().minusDays(1));
        dependenteRepository.save(expirado);

        mockMvc.perform(get("/pre-guias/pacientes").header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Antonio Vigencia"));
    }

    private Beneficiario titular() {
        Usuario usuario = new Usuario();
        usuario.setLogin(LOGIN_TITULAR);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario = usuarioRepository.save(usuario);
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome("Antonio Vigencia");
        b.setPrecCp("45872213377");
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("12987654321");
        return beneficiarioRepository.save(b);
    }
}
