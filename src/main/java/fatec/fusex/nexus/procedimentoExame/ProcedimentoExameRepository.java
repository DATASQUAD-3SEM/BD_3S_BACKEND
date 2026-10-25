package fatec.fusex.nexus.procedimentoExame;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso ao catalogo de procedimentos, com busca por nome ou codigo TUSS.
 */
public interface ProcedimentoExameRepository extends JpaRepository<ProcedimentoExame, Long> {

    List<ProcedimentoExame> findByCodigoTuss(String codigoTuss);

    // T2-BE4: confere de uma vez se os codigos enviados existem no catalogo
    List<ProcedimentoExame> findByCodigoTussIn(Collection<String> codigosTuss);

    // T2-BE3: busca no catalogo inteiro (sem filtro de OCS). O limite e a ordem vem do Pageable.
    @Query("SELECT p FROM ProcedimentoExame p WHERE LOWER(p.terminologiaProcedimento) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!' OR LOWER(p.codigoTuss) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'")
    List<ProcedimentoExame> buscarPorTermo(@Param("busca") String busca, Pageable pageable);
}
