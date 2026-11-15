package fatec.fusex.nexus.espelho;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EspelhoItemRepository extends JpaRepository<EspelhoItem, Long> {

    @Query("""
            select i from EspelhoItem i
            join fetch i.guiaProcedimento gp
            join fetch gp.guia g
            join fetch g.preGuia pg
            join fetch pg.beneficiario
            where i.espelho.idEspelho = :idEspelho
            order by g.idGuia asc, gp.idGuiaProcedimento asc
            """)
    List<EspelhoItem> buscarComDadosPorEspelho(@Param("idEspelho") Long idEspelho);

    List<EspelhoItem> findByEspelhoIdEspelhoAndGuiaProcedimentoGuiaIdGuiaIn(Long idEspelho,
                                                                            Collection<Long> guiaIds);

    boolean existsByEspelhoIdEspelho(Long idEspelho);
}