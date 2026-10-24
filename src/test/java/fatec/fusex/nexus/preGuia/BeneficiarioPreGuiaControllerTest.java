package fatec.fusex.nexus.preguia;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse;

@WebMvcTest(BeneficiarioPreGuiaController.class)
class BeneficiarioPreGuiaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreGuiaService preGuiaService;

    @Test
    @WithMockUser(username = "123", roles = "BENEFICIARIO")
    void buscarCorrecao_quandoSucesso_retorna200() throws Exception {
        when(preGuiaService.buscarDadosCorrecao(eq(1L), eq("123")))
            .thenReturn(new CorrecaoPreGuiaResponse(
                "PRE-1", 
                "ATENCAO", 
                new CorrecaoPreGuiaResponse.BeneficiarioResumo("Fulano", "123"), 
                null, null, null, null
            ));

        mockMvc.perform(get("/beneficiarios/me/pre-guias/1/correcao"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero").value("PRE-1"));
    }

    @Test
    @WithMockUser(username = "123", roles = "MEDICO")
    void buscarCorrecao_quandoPerfilErrado_retorna403() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias/1/correcao"))
            .andExpect(status().isForbidden());
    }
}