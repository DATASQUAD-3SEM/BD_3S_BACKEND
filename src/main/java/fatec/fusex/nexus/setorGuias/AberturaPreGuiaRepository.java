package fatec.fusex.nexus.setorGuias;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.preGuia.PreGuia;
import jakarta.persistence.LockModeType;

/** T4-BE3: repositorio proprio da abertura, para travar a pre-guia enquanto o status muda. */
public interface AberturaPreGuiaRepository extends JpaRepository<PreGuia, Long> {

    // SELECT ... FOR UPDATE: uma segunda abertura simultanea espera a primeira terminar
    // e ja encontra a pre-guia em EM_ANALISE, entao so uma linha de historico e gravada.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreGuia p WHERE p.idPreGuia = :id")
    Optional<PreGuia> buscarParaAbrir(@Param("id") Long id);
}
