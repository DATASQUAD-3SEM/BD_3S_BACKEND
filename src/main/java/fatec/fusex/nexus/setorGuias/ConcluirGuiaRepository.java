package fatec.fusex.nexus.setorGuias;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.preGuia.PreGuia;
import jakarta.persistence.LockModeType;

/** T4-BE6: repositorio proprio da conclusao da Guia, com as consultas que a task precisa. */
public interface ConcluirGuiaRepository extends JpaRepository<Guia, Long> {

    // SELECT ... FOR UPDATE: se dois cliques chegarem juntos, o segundo espera e ja encontra a Guia criada (409)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreGuia p WHERE p.idPreGuia = :id")
    Optional<PreGuia> buscarPreGuiaParaConcluir(@Param("id") Long id);

    @Query("SELECT r FROM GuiaRascunho r WHERE r.preGuiaId = :idPreGuia")
    Optional<GuiaRascunho> buscarRascunho(@Param("idPreGuia") Long idPreGuia);

    // Cada pre-guia gera uma so Guia (guia.pre_guia_id e unico)
    @Query("SELECT COUNT(g) > 0 FROM Guia g WHERE g.preGuia.idPreGuia = :idPreGuia")
    boolean temGuia(@Param("idPreGuia") Long idPreGuia);

    @Query("SELECT COUNT(g) > 0 FROM Guia g WHERE g.numero = :numero")
    boolean numeroUsadoEmGuia(@Param("numero") String numero);
}
