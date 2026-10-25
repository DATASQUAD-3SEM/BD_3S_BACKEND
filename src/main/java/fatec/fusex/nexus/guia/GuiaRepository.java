package fatec.fusex.nexus.guia;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;

/**
 * Acesso as Guias, incluindo as consultas de Guias pendentes de assinatura e as buscas da recepcao.
 */
public interface GuiaRepository extends JpaRepository<Guia, Long> {

    Optional<Guia> findByPreGuiaIdPreGuia(Long idPreGuia);

    @Query("""
            select g from Guia g
            join fetch g.preGuia pg
            join fetch pg.beneficiario b
            join fetch pg.ocs
            where pg.status = :status
              and (lower(b.nome) like lower(concat('%', :termo, '%'))
                   or g.numero like concat('%', :termo, '%')
                   or (:precCp is not null and b.precCp like concat('%', :precCp, '%')))
            order by g.dataInsercao asc, g.idGuia asc
            """)
    List<Guia> buscarConcluidasParaRecepcao(@Param("status") PreGuiaStatus status,
                                            @Param("termo") String termo,
                                            @Param("precCp") String precCp,
                                            Pageable pageable);

    @Query("""
            select g from Guia g
            join fetch g.preGuia pg
            join fetch pg.beneficiario b
            where not exists (
                select a.idAssinaturaGuia from AssinaturaGuia a
                where a.guia = g and a.papel = :papel)
            order by g.dataInsercao asc, g.idGuia asc
            """)
    List<Guia> buscarSemAssinatura(@Param("papel") AssinaturaGuiaPapel papel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Guia g where g.idGuia = :id")
    Optional<Guia> findByIdComLock(@Param("id") Long id);
}