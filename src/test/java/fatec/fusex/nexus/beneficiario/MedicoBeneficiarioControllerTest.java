package fatec.fusex.nexus.beneficiario;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

class MedicoBeneficiarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BeneficiarioService beneficiarioService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoBeneficiarioController(beneficiarioService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    /** Simula um usuario logado com o perfil dado (o login/JWT real ainda nao existe). */
    private static RequestPostProcessor comPerfil(PerfilUsuario perfil) {
        return requisicao -> {
            requisicao.addUserRole(perfil.name());
            return requisicao;
        };
    }

    private static Beneficiario beneficiario() {
        Usuario usuario = new Usuario();
        usuario.setLogin("52998224725");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);

        Beneficiario b = new Beneficiario();
        b.setUsuarioId(7L);
        b.setUsuario(usuario);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp("45872213300");
        // 57 anos e 1 dia: a idade continua 57 mesmo que o teste vire a meia-noite
        b.setDataNascimento(LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusYears(57).minusDays(1));
        b.setTelefone("12987654321");
        return b;
    }

    @Test
    void buscarPorPrecCp_comoMedico_devolve200ComOsCamposDoContrato() throws Exception {
        when(beneficiarioService.buscarAtivoPorPrecCp("45872213300")).thenReturn(beneficiario());

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "45872213300")
                        .with(comPerfil(PerfilUsuario.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$.grupo").value(BeneficiarioMedicoResponse.GRUPO))
                .andExpect(jsonPath("$.faixaEtaria").value("51 a 60"))
                .andExpect(jsonPath("$.idade").value(57))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.telefone").value("12987654321"));
    }

    @Test
    void buscarPorPrecCp_comMascara_repassaOValorAoService() throws Exception {
        when(beneficiarioService.buscarAtivoPorPrecCp("458.722.133-00")).thenReturn(beneficiario());

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "458.722.133-00")
                        .with(comPerfil(PerfilUsuario.MEDICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(beneficiarioService).buscarAtivoPorPrecCp("458.722.133-00");
    }

    @Test
    void buscarPorPrecCp_naoExpoeDadosAlemDoContrato() throws Exception {
        when(beneficiarioService.buscarAtivoPorPrecCp(anyString())).thenReturn(beneficiario());

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "45872213300")
                        .with(comPerfil(PerfilUsuario.MEDICO)))
                .andExpect(jsonPath("$.precCp").doesNotExist())
                .andExpect(jsonPath("$.dataNascimento").doesNotExist())
                .andExpect(content().string(not(containsString("senha"))));
    }

    @Test
    void buscarPorPrecCp_quandoNaoExisteOuInativo_devolve404() throws Exception {
        when(beneficiarioService.buscarAtivoPorPrecCp(anyString()))
                .thenThrow(new RecursoNaoEncontradoException("Beneficiario nao encontrado"));

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "45872213300")
                        .with(comPerfil(PerfilUsuario.MEDICO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Beneficiario nao encontrado"));
    }

    @Test
    void buscarPorPrecCp_quandoFormatoInvalido_devolve400() throws Exception {
        when(beneficiarioService.buscarAtivoPorPrecCp(anyString()))
                .thenThrow(new RegraDeNegocioException("Prec CP invalido"));

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "123")
                        .with(comPerfil(PerfilUsuario.MEDICO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void buscarPorPrecCp_quandoParametroAusente_devolve400() throws Exception {
        when(beneficiarioService.buscarAtivoPorPrecCp(null))
                .thenThrow(new RegraDeNegocioException("Informe o Prec CP"));

        mockMvc.perform(get("/medicos/beneficiarios")
                        .with(comPerfil(PerfilUsuario.MEDICO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe o Prec CP"));
    }

    @Test
    void buscarPorPrecCp_comPerfilQueNaoEMedico_devolve403() throws Exception {
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            if (perfil == PerfilUsuario.MEDICO) {
                continue;
            }
            mockMvc.perform(get("/medicos/beneficiarios")
                            .param("precCp", "45872213300")
                            .with(comPerfil(perfil)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
        verifyNoInteractions(beneficiarioService);
    }

    @Test
    void buscarPorPrecCp_semUsuarioLogado_devolve403() throws Exception {
        mockMvc.perform(get("/medicos/beneficiarios").param("precCp", "45872213300"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(beneficiarioService);
    }

    @Test
    void buscarPorPrecCp_perfilErradoComFormatoInvalido_devolve403EnaoRevelaValidacao() throws Exception {
        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "lixo")
                        .with(comPerfil(PerfilUsuario.RECEPCAO)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(beneficiarioService);
    }
}
