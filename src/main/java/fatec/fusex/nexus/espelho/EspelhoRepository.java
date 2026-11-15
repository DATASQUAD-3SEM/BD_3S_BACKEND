package fatec.fusex.nexus.espelho;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EspelhoRepository extends JpaRepository<Espelho, Long> {

    Optional<Espelho> findByOcsIdOcsAndStatus(Long idOcs, StatusEspelho status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espelho e where e.ocs.idOcs = :idOcs and e.status = :status")
    Optional<Espelho> buscarComLockPorOcsEStatus(@Param("idOcs") Long idOcs,
                                                 @Param("status") StatusEspelho status);

    List<Espelho> findByOcsIdOcsAndStatusOrderByDataEnvioDescIdEspelhoDesc(Long idOcs, StatusEspelho status);
}