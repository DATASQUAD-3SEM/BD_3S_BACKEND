package fatec.fusex.nexus.medicoFusex;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicoFusexRepository extends JpaRepository<MedicoFusex, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<MedicoFusex> findByUsuarioLogin(String login);

    @EntityGraph(attributePaths = "usuario")
    Optional<MedicoFusex> findByUsuarioLoginAndUsuarioAtivoTrue(String login);
}