package fatec.fusex.nexus.usuario;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso as contas de usuario, incluindo a busca pelo login.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);
}