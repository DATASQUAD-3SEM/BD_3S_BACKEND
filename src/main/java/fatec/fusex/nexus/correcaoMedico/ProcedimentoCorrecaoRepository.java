package fatec.fusex.nexus.correcaoMedico;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;

/**
 * T14-BE4: repositorio proprio da tela de correcao, sem reaproveitar o do T2-BE3,
 * para as tasks ficarem independentes. Busca no catalogo inteiro, sem filtro pela OCS.
 * Maiusculas e acentos sao ignorados pelo collation utf8mb4_unicode_ci da tabela (migration V5).
 */
public interface ProcedimentoCorrecaoRepository extends JpaRepository<ProcedimentoExame, Long> {

    @Query("""
            SELECT p FROM ProcedimentoExame p
            WHERE LOWER(p.terminologiaProcedimento) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
               OR LOWER(p.codigoTuss) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
            """)
    List<ProcedimentoExame> buscarNoCatalogo(@Param("busca") String busca, Pageable pageable);
}
