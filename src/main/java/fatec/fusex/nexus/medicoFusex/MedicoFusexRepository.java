package fatec.fusex.nexus.medicoFusex;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicoFusexRepository extends JpaRepository<MedicoFusex, Long> {
    Optional<MedicoFusex> findByUsuarioLogin(String login);
    Optional<MedicoFusex> findByUsuarioLoginAndUsuarioAtivoTrue(String login);
}