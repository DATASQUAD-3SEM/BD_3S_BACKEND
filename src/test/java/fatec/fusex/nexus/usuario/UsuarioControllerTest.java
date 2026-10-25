package fatec.fusex.nexus.usuario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.MuitasTentativasException;
import fatec.fusex.nexus.usuario.dto.AlterarSenhaRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes HTTP de POST /usuarios/me/senha (T13-BE1).
 */
@WebMvcTest(UsuarioController.class)
@Import(SimulacaoLogin.class)
class UsuarioControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    UsuarioControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void alterarSenha_quandoSucesso_devolve200() throws Exception {
        doNothing().when(usuarioService)
                .alterarSenha(any(AlterarSenhaRequest.class));

        mockMvc.perform(post("/usuarios/me/senha")
                        .header(SimulacaoLoginFilter.HEADER, "usuario-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "SenhaAtual123",
                                  "novaSenha": "SenhaNova456",
                                  "confirmacao": "SenhaNova456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senhaAlterada").value(true));
    }

    @Test
    void alterarSenha_quandoSenhaAtualIncorreta_devolve422() throws Exception {
        doThrow(new DadosInvalidosException(
                "senhaAtual", "Senha atual incorreta"))
                .when(usuarioService)
                .alterarSenha(any(AlterarSenhaRequest.class));

        mockMvc.perform(post("/usuarios/me/senha")
                        .header(SimulacaoLoginFilter.HEADER, "usuario-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "Errada123",
                                  "novaSenha": "SenhaNova456",
                                  "confirmacao": "SenhaNova456"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]")
                        .value("senhaAtual: Senha atual incorreta"));
    }

    @Test
    void alterarSenha_quandoBloqueado_devolve429ComTempoDeEspera() throws Exception {
        doThrow(new MuitasTentativasException())
                .when(usuarioService)
                .alterarSenha(any(AlterarSenhaRequest.class));

        mockMvc.perform(post("/usuarios/me/senha")
                        .header(SimulacaoLoginFilter.HEADER, "usuario-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "SenhaAtual123",
                                  "novaSenha": "SenhaNova456",
                                  "confirmacao": "SenhaNova456"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }
}