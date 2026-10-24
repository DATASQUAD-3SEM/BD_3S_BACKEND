package fatec.fusex.nexus.ocs;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OcsRepository extends JpaRepository<Ocs, Long> {

    @Query("SELECT o FROM Ocs o WHERE o.ativo = true AND (LOWER(o.nome) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!' OR LOWER(o.contratoNum) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!' OR LOWER(CAST(o.tipo AS String)) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!')")
    Page<Ocs> buscarPorTermo(@Param("busca") String busca, Pageable pageable);
    Page<Ocs> findByAtivoTrue(Pageable pageable);

    List<Ocs> findByAtivoTrueOrderByNomeAsc();

}