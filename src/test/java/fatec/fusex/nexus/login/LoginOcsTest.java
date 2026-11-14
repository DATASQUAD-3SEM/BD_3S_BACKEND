package fatec.fusex.nexus.login;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.login.dto.LoginRequest;
import fatec.fusex.nexus.login.dto.LoginResponse;
import fatec.fusex.nexus.ocs.ContaOcs;
import fatec.fusex.nexus.ocs.ContaOcsRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/**
 * Login de uma conta OCS: entra com o perfil OCS e o nome exibido e o da OCS vinculada em ContaOcs.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginOcsTest {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ContaOcsRepository contaOcsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    @Test
    void contaOcsEntraComPerfilOcsENomeDaOcs() {
        Usuario usuario = new Usuario();
        usuario.setLogin("ocs.login");
        usuario.setSenhaHash(passwordEncoder.encode("senha123"));
        usuario.setPerfil(PerfilUsuario.OCS);
        em.persist(usuario);

        Ocs ocs = new Ocs();
        ocs.setNome("Laboratorio Login");
        ocs.setContratoNum("LOGIN-OCS-1");
        ocs.setTipo(OcsTipo.OCS);
        em.persist(ocs);

        ContaOcs conta = new ContaOcs();
        conta.setUsuario(usuario);
        conta.setOcs(ocs);
        contaOcsRepository.save(conta);
        em.flush();

        LoginResponse resposta = loginService.entrar(new LoginRequest("ocs.login", "senha123"));

        assertThat(resposta.perfil()).isEqualTo("OCS");
        assertThat(resposta.nome()).isEqualTo("Laboratorio Login");
    }
}
