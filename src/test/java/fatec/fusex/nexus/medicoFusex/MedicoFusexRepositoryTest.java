package fatec.fusex.nexus.medicoFusex;

import static org.assertj.core.api.Assertions.assertThat;

import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository; // Assumindo que você tem esse repository
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de gravacao e leitura do medico no banco em memoria.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MedicoFusexRepositoryTest {

    private final MedicoFusexRepository medicoRepository;

    private final UsuarioRepository usuarioRepository;

    MedicoFusexRepositoryTest(MedicoFusexRepository medicoRepository, UsuarioRepository usuarioRepository) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @BeforeEach
    void setup() {
        medicoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void findByUsuarioLogin_quandoExiste_retornaMedico() {
        Usuario usuario = new Usuario();
        usuario.setLogin("11144477735");
        usuario.setSenhaHash("hash");
        usuario.setPerfil(PerfilUsuario.MEDICO);
        usuario.setAtivo(true);
        usuarioRepository.saveAndFlush(usuario); // <--- MUDOU AQUI

        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario);
        medico.setNome("Carlos Eduardo");
        medico.setPatente("Capitao");
        medico.setCrm("123456-SP");
        medico.setPrecCp("11122233344");
        medicoRepository.saveAndFlush(medico); // <--- MUDOU AQUI

        var resultado = medicoRepository.findByUsuarioLogin("11144477735");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Carlos Eduardo");
    }

    @Test
    void findByUsuarioLoginAndUsuarioAtivoTrue_quandoInativo_retornaVazio() {
        Usuario usuario = new Usuario();
        usuario.setLogin("11144477735");
        usuario.setSenhaHash("hash");
        usuario.setPerfil(PerfilUsuario.MEDICO);
        usuario.setAtivo(false);
        usuarioRepository.saveAndFlush(usuario); // <--- MUDOU AQUI

        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario);
        medico.setNome("Carlos Eduardo");
        medico.setPatente("Capitao");
        medico.setCrm("123456-SP");
        medico.setPrecCp("11122233344");
        medicoRepository.saveAndFlush(medico); // <--- MUDOU AQUI

        var resultado = medicoRepository.findByUsuarioLoginAndUsuarioAtivoTrue("11144477735");

        assertThat(resultado).isEmpty();
    }
}