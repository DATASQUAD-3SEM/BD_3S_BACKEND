package fatec.fusex.nexus.common.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Conta OCS nao acessa as telas dos outros perfis: cada rota responde 403 para o perfil OCS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PerfilOcsAcessoTest {

    private static final String LOGIN_OCS = "ocs.acesso";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void contaOcs() {
        Usuario usuario = new Usuario();
        usuario.setLogin(LOGIN_OCS);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.OCS);
        em.persist(usuario);
        em.flush();
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/beneficiarios/me",
            "GET,/chefe/beneficiarios",
            "GET,/medicos/me/perfil",
            "GET,/setor-guias/pre-guias",
            "GET,/recepcao/guias",
            "POST,/usuarios/me/senha"
    })
    void rotasDeOutrosPerfisRespondem403ParaOcs(String metodo, String caminho) throws Exception {
        var requisicao = "POST".equals(metodo) ? post(caminho) : get(caminho);

        mockMvc.perform(requisicao
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_OCS)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
