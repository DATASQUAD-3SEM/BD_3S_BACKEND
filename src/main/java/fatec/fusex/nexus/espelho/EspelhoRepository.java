package fatec.fusex.nexus.espelho;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EspelhoRepository extends JpaRepository<Espelho, Long> {

    Optional<Espelho> findByOcsIdOcsAndStatus(Long idOcs, StatusEspelho status);
}