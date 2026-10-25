package fatec.fusex.nexus.MinhasPreGuias;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.preGuia.PreGuia;
import jakarta.persistence.LockModeType;

/** T9-BE4: repositorio proprio do reenvio, para travar a pre-guia enquanto ela volta para Pendente. */
public interface ReenvioPreGuiaRepository extends JpaRepository<PreGuia, Long> {

    // SELECT ... FOR UPDATE: um segundo reenvio simultaneo espera o primeiro e ja encontra Pendente (409)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreGuia p WHERE p.idPreGuia = :id")
    Optional<PreGuia> buscarParaReenviar(@Param("id") Long id);
}
