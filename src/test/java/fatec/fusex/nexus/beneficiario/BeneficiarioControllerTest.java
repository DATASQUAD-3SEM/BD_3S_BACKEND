package fatec.fusex.nexus.beneficiario;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;

/**
 * Testes HTTP de GET /beneficiarios/me e PATCH /beneficiarios/me/telefone (T10-BE1 e T10-BE2), com o servico simulado.
 */
@WebMvcTest(BeneficiarioController.class)
@Import(SimulacaoLogin.class)
class BeneficiarioControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean private BeneficiarioService beneficiarioService;

    BeneficiarioControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void me_quandoTudoCerto_devolve200ComDados() throws Exception {
        BeneficiarioResponse resposta = new BeneficiarioResponse(
                "Antonio Carlos", "Exercito", "52998224725", "45872213300",
                LocalDate.of(1968, 3, 15), 58, "51 a 60", "12987654321", false);
        when(beneficiarioService.buscarLogado()).thenReturn(resposta);

        mockMvc.perform(get("/beneficiarios/me")
                        .header(SimulacaoLoginFilter.HEADER, "52998224725"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Antonio Carlos"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.precCp").value("45872213300"))
                .andExpect(jsonPath("$.telefone").value("12987654321"))
                .andExpect(jsonPath("$.dataNascimento").value("1968-03-15"))
                .andExpect(jsonPath("$.idade").value(58))
                .andExpect(jsonPath("$.faixaEtaria").value("51 a 60"))
                .andExpect(jsonPath("$.grupo").value("Exercito"))
                .andExpect(jsonPath("$.assinaturaCadastrada").value(false));
    }

    @Test
    void me_quandoAssinaturaCadastrada_retornaTrue() throws Exception {
        BeneficiarioResponse resposta = new BeneficiarioResponse(
                "Antonio Carlos", "Exercito", "52998224725", "45872213300",
                LocalDate.of(1968, 3, 15), 58, "51 a 60", "12987654321", true);
        when(beneficiarioService.buscarLogado()).thenReturn(resposta);

        mockMvc.perform(get("/beneficiarios/me")
                        .header(SimulacaoLoginFilter.HEADER, "52998224725"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assinaturaCadastrada").value(true));
    }

    @Test
    void me_quandoPerfilErrado_devolve403() throws Exception {
        when(beneficiarioService.buscarLogado())
                .thenThrow(new AcessoNegadoException("perfil errado"));

        mockMvc.perform(get("/beneficiarios/me")
                        .header(SimulacaoLoginFilter.HEADER, "11144477735"))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_quandoSemHeader_devolve401() throws Exception {
        when(beneficiarioService.buscarLogado())
                .thenThrow(new NaoAutenticadoException("sem login"));

        mockMvc.perform(get("/beneficiarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void atualizarTelefone_comCamposExtrasIgnoraOsOutrosCampos() throws Exception {
        when(beneficiarioService.atualizarTelefoneLogado("(12) 98765-4321"))
                .thenReturn("12987654321");

        mockMvc.perform(patch("/beneficiarios/me/telefone")
                        .header(SimulacaoLoginFilter.HEADER, "52998224725")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "telefone": "(12) 98765-4321",
                                "nome": "Nome alterado indevidamente",
                                "perfil": "CHEFE",
                                "cpf": "00000000000"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("12987654321"));

        verify(beneficiarioService)
                .atualizarTelefoneLogado("(12) 98765-4321");
    }

    @Test
    void atualizarTelefone_comFormatoInvalido_devolve422() throws Exception {
        when(beneficiarioService.atualizarTelefoneLogado("123"))
                .thenThrow(new DadosInvalidosException(
                        "telefone",
                        "Informe um telefone celular valido com DDD."
                ));

        mockMvc.perform(patch("/beneficiarios/me/telefone")
                        .header(SimulacaoLoginFilter.HEADER, "52998224725")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "telefone": "123"
                            }
                            """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detalhes[0]")
                        .value("telefone: Informe um telefone celular valido com DDD."));
    }

    @Test
    void atualizarTelefone_comPerfilErrado_devolve403() throws Exception {
        when(beneficiarioService.atualizarTelefoneLogado("12987654321"))
                .thenThrow(new AcessoNegadoException(
                        "Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO"
                ));

        mockMvc.perform(patch("/beneficiarios/me/telefone")
                        .header(SimulacaoLoginFilter.HEADER, "11144477735")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "telefone": "12987654321"
                            }
                            """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}