package fatec.fusex.nexus.setorGuias;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.preGuia.PreGuia;
import jakarta.persistence.LockModeType;

/** T4-BE4: repositorio proprio do pedido de correcao, para travar a pre-guia enquanto o status muda. */
public interface RequererCorrecaoRepository extends JpaRepository<PreGuia, Long> {

    // SELECT ... FOR UPDATE: um segundo pedido simultaneo espera o primeiro e ja encontra ATENCAO (409)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreGuia p WHERE p.idPreGuia = :id")
    Optional<PreGuia> buscarParaCorrigir(@Param("id") Long id);
}
