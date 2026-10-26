package fatec.fusex.nexus.assinaturaGuia;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso as assinaturas das Guias. Usado para saber quais papeis ja assinaram cada Guia.
 */
public interface AssinaturaGuiaRepository extends JpaRepository<AssinaturaGuia, Long> {

    long countByGuiaPreGuiaIdPreGuia(Long idPreGuia);

    List<AssinaturaGuia> findByGuiaIdGuiaIn(Collection<Long> guiaIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AssinaturaGuia a where a.guia.idGuia = :guiaId")
    List<AssinaturaGuia> buscarComLockPorGuia(@Param("guiaId") Long guiaId);
}