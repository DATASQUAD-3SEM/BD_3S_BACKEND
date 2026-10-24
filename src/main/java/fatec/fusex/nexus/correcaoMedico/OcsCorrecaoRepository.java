package fatec.fusex.nexus.correcaoMedico;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.ocs.Ocs;

/**
 * T14-BE3: repositorio proprio da tela de correcao, sem reaproveitar o OcsRepository do T2-BE2,
 * para as tasks ficarem independentes. Maiusculas e acentos sao ignorados pelo collation
 * utf8mb4_unicode_ci da tabela Ocs (migration V4).
 */
public interface OcsCorrecaoRepository extends JpaRepository<Ocs, Long> {

    @Query("""
            SELECT o FROM Ocs o
            WHERE o.ativo = true
              AND (LOWER(o.nome) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
                   OR LOWER(o.contratoNum) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
                   OR LOWER(CAST(o.tipo AS String)) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!')
            """)
    Page<Ocs> buscarAtivasPorTermo(@Param("busca") String busca, Pageable pageable);

    @Query("SELECT o FROM Ocs o WHERE o.ativo = true")
    Page<Ocs> listarAtivas(Pageable pageable);
}
