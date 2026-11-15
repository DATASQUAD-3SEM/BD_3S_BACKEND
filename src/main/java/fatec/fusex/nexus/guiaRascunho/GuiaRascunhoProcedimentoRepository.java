package fatec.fusex.nexus.guiaRascunho;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Exames do rascunho da Guia, por pre-guia (T22-BE1). */
public interface GuiaRascunhoProcedimentoRepository
        extends JpaRepository<GuiaRascunhoProcedimento, GuiaRascunhoProcedimentoId> {

    // Na ordem gravada
    @Query("SELECT p FROM GuiaRascunhoProcedimento p WHERE p.id.preGuiaId = :idPreGuia ORDER BY p.id.ordem")
    List<GuiaRascunhoProcedimento> listarDaPreGuia(@Param("idPreGuia") Long idPreGuia);
}
