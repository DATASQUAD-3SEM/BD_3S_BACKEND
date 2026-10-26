package fatec.fusex.nexus.correcaoMedico;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.preGuia.PreGuia;
import jakarta.persistence.LockModeType;

/** T14-BE5: repositorio proprio da correcao, para travar a pre-guia durante o reenvio. */
public interface PreGuiaCorrecaoRepository extends JpaRepository<PreGuia, Long> {

    // SELECT ... FOR UPDATE: um segundo reenvio simultaneo espera o primeiro terminar
    // e ja encontra a pre-guia em PENDENTE (409), entao so uma mudanca e gravada.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreGuia p WHERE p.idPreGuia = :id")
    Optional<PreGuia> buscarParaCorrigir(@Param("id") Long id);
}
