package fatec.fusex.nexus.common.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PerfilSetoresFaturaAcessoTest {

    private static final List<PerfilUsuario> PERFIS_DE_SETOR =
            List.of(PerfilUsuario.SETOR_FATURA, PerfilUsuario.RASTREABILIDADE, PerfilUsuario.AUDITORIA);

    private static final List<String[]> ROTAS = List.of(
            new String[]{"GET", "/beneficiarios/me"},
            new String[]{"GET", "/chefe/beneficiarios"},
            new String[]{"GET", "/medicos/me/perfil"},
            new String[]{"GET", "/setor-guias/pre-guias"},
            new String[]{"GET", "/recepcao/guias"},
            new String[]{"GET", "/ocs/espelho"},
            new String[]{"POST", "/usuarios/me/senha"},
            new String[]{"POST", "/setor-guias/pre-guias/1/guia/concluir"},
            new String[]{"PUT", "/setor-guias/pre-guias/1/guia-rascunho"});

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void contasDeSetor() {
        for (PerfilUsuario perfil : PERFIS_DE_SETOR) {
            Usuario usuario = new Usuario();
            usuario.setLogin(loginDe(perfil));
            usuario.setSenhaHash("hash-de-teste");
            usuario.setPerfil(perfil);
            em.persist(usuario);
        }
        em.flush();
    }

    static Stream<Arguments> combinacoes() {
        return PERFIS_DE_SETOR.stream()
                .flatMap(perfil -> ROTAS.stream()
                        .map(rota -> Arguments.of(perfil, rota[0], rota[1])));
    }

    @ParameterizedTest
    @MethodSource("combinacoes")
    void perfilDeSetorRecebe403NasRotasDeOutrosPerfis(PerfilUsuario perfil, String metodo, String caminho)
            throws Exception {
        var requisicao = switch (metodo) {
            case "POST" -> post(caminho);
            case "PUT" -> put(caminho).contentType(MediaType.MULTIPART_FORM_DATA);
            default -> get(caminho);
        };

        if (!"PUT".equals(metodo)) {
            requisicao = requisicao.contentType("application/json").content("{}");
        }

        mockMvc.perform(requisicao
                        .header(SimulacaoLoginFilter.HEADER, loginDe(perfil)))
                .andExpect(status().isForbidden());
    }

    private static String loginDe(PerfilUsuario perfil) {
        return "acesso." + perfil.name().toLowerCase();
    }
}