package fatec.fusex.nexus.login;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.login.dto.LoginRequest;
import fatec.fusex.nexus.login.dto.LoginResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginSetoresFaturaTest {

    @Autowired
    private LoginService loginService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    @ParameterizedTest
    @CsvSource({
            "SETOR_FATURA, Setor de Faturas",
            "RASTREABILIDADE, Rastreabilidade",
            "AUDITORIA, Auditoria"
    })
    void contaDeSetorEntraComSeuPerfilENomeDoSetor(PerfilUsuario perfil, String nomeEsperado) {
        String login = "login." + perfil.name().toLowerCase();

        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash(passwordEncoder.encode("senha123"));
        usuario.setPerfil(perfil);
        em.persist(usuario);
        em.flush();

        LoginResponse resposta = loginService.entrar(new LoginRequest(login, "senha123"));

        assertThat(resposta.perfil()).isEqualTo(perfil.name());
        assertThat(resposta.nome()).isEqualTo(nomeEsperado);
    }
}