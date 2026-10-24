package fatec.fusex.nexus.preGuia;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreGuiaRepository extends JpaRepository<PreGuia, Long> {

    List<PreGuia> findByBeneficiarioUsuarioId(Long usuarioId);

    List<PreGuia> findByStatus(PreGuiaStatus status);

    List<PreGuia> findByBeneficiarioUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    // T14-BE1: pre-guias do medico emissor em um status, da mais antiga para a mais recente
    @EntityGraph(attributePaths = {"beneficiario", "ocs"})
    List<PreGuia> findByMedicoFusexUsuarioIdAndStatusOrderByDataCriacaoAscIdPreGuiaAsc(
            Long medicoId, PreGuiaStatus status);


    @EntityGraph(attributePaths = {"beneficiario", "ocs", "medicoFusex"})
    @Query("""

            SELECT p FROM PreGuia p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:busca = '' OR
               REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
               REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
               REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
               REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
               REPLACE(REPLACE(REPLACE(
                 LOWER(p.beneficiario.nome),
                 'á','a'),'à','a'),'â','a'),
                 'ã','a'),'ä','a'),
                 'é','e'),'è','e'),'ê','e'),'ë','e'),
                 'í','i'),'ì','i'),'î','i'),'ï','i'),
                 'ó','o'),'ò','o'),'ô','o'),'õ','o'),'ö','o'),
                 'ú','u'),'ù','u'),'û','u'),'ü','u'),
                 'ç','c')
               LIKE CONCAT('%', :busca, '%')
               OR LOWER(p.beneficiario.precCp) LIKE CONCAT('%', :busca, '%'))
        """)
    Page<PreGuia> buscarFila(
            @Param("status") PreGuiaStatus status,
            @Param("busca") String busca,
            Pageable pageable);

   }