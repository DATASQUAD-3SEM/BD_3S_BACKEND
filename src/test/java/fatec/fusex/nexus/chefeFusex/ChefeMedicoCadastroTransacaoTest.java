package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import fatec.fusex.nexus.chefeFusex.dto.CadastrarMedicoRequest;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Falha de transacao: se o insert do medico falhar, o usuario tambem nao pode ficar gravado.
 * Sem @Transactional no teste, para o rollback ser o do proprio service.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChefeMedicoCadastroTransacaoTest {

    private static final String CHEFE = "chefe.transacao";
    private static final String CPF_NOVO = "39053344705";

    private final ChefeMedicoCadastroService service;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    @MockitoSpyBean
    private MedicoFusexRepository medicoRepository;

    ChefeMedicoCadastroTransacaoTest(
            ChefeMedicoCadastroService service,
            UsuarioRepository usuarioRepository,
            SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @BeforeEach
    void dados() {
        Usuario chefe = new Usuario();
        chefe.setLogin(CHEFE);
        chefe.setSenhaHash("hash");
        chefe.setPerfil(PerfilUsuario.CHEFE_FUSEX);
        chefe.setAtivo(true);
        usuarioRepository.saveAndFlush(chefe);
        simulacaoLogin.definir(CHEFE);
    }

    @AfterEach
    void limpar() {
        simulacaoLogin.limpar();
        usuarioRepository.findByLogin(CPF_NOVO).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(CHEFE).ifPresent(usuarioRepository::delete);
    }

    @Test
    void falhaAoGravarMedico_naoGravaOUsuario() {
        doThrow(new IllegalStateException("falha simulada no insert do medico"))
                .when(medicoRepository).saveAndFlush(any(MedicoFusex.class));

        CadastrarMedicoRequest request = new CadastrarMedicoRequest(
                "Marta Souza Lima", "Major", CPF_NOVO, "45810020001", "654321-RJ", "senha1234");

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(IllegalStateException.class);

        assertThat(usuarioRepository.findByLogin(CPF_NOVO)).isEmpty();
    }
}
