package fatec.fusex.nexus.beneficiario;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;

@WebMvcTest(BeneficiarioController.class)
@Import(SimulacaoLogin.class)
class BeneficiarioControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BeneficiarioService beneficiarioService;

    @Test
    void me_quandoTudoCerto_devolve200ComDados() throws Exception {
        BeneficiarioResponse resposta = new BeneficiarioResponse(
                "Antonio Carlos", "52998224725", "45872213300", "12987654321",
                LocalDate.of(1968, 3, 15), "BENEFICIARIO", false);
        when(beneficiarioService.buscarLogado()).thenReturn(resposta);

        mockMvc.perform(get("/beneficiarios/me")
                        .header(SimulacaoLoginFilter.HEADER, "52998224725"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Antonio Carlos"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.precCp").value("45872213300"))
                .andExpect(jsonPath("$.telefone").value("12987654321"))
                .andExpect(jsonPath("$.dataNascimento").value("1968-03-15"))
                .andExpect(jsonPath("$.grupo").value("BENEFICIARIO"))
                .andExpect(jsonPath("$.assinaturaCadastrada").value(false));
    }

    @Test
    void me_quandoAssinaturaCadastrada_retornaTrue() throws Exception {
        BeneficiarioResponse resposta = new BeneficiarioResponse(
                "Antonio Carlos", "52998224725", "45872213300", "12987654321",
                LocalDate.of(1968, 3, 15), "BENEFICIARIO", true);
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
}