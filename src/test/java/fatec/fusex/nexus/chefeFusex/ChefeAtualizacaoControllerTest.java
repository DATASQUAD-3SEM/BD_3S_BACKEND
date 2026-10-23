package fatec.fusex.nexus.chefeFusex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fatec.fusex.nexus.chefeFusex.dto.AtualizarChefeRequest;
import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.ConflitoException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChefeAtualizacaoControllerTest {

    @Mock
    private ChefePerfilService chefePerfilService;

    private MockMvc mockMvc;

    @BeforeEach
    void prepararController() {
        ChefePerfilController controller =
                new ChefePerfilController(chefePerfilService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void atualizarMeuPerfil_quandoDadosValidos_devolve200() throws Exception {
        when(chefePerfilService.atualizarPerfil(
                any(AtualizarChefeRequest.class)
        )).thenReturn(
                new ChefePerfilResponse(
                        "Nome Novo",
                        "Major",
                        "chefe-novo",
                        true
                )
        );

        mockMvc.perform(put("/chefe/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Nome Novo",
                                  "patente": "Major",
                                  "login": "chefe-novo",
                                  "novaSenha": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"))
                .andExpect(jsonPath("$.patente").value("Major"))
                .andExpect(jsonPath("$.login").value("chefe-novo"))
                .andExpect(jsonPath("$.assinaturaCadastrada").value(true));
    }

    @Test
    void atualizarMeuPerfil_quandoLoginDuplicado_devolve409() throws Exception {
        when(chefePerfilService.atualizarPerfil(
                any(AtualizarChefeRequest.class)
        )).thenThrow(
                new ConflitoException("Já existe um usuário com esse login.")
        );

        mockMvc.perform(put("/chefe/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Nome Novo",
                                  "patente": "Major",
                                  "login": "login-existente",
                                  "novaSenha": ""
                                }
                                """))
                .andExpect(status().isConflict());
    }
}