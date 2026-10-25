package fatec.fusex.nexus.preGuiaProcedimento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreGuiaProcedimentoRepository extends JpaRepository<PreGuiaProcedimento, PreGuiaProcedimentoId> {

    List<PreGuiaProcedimento> findByPreGuiaIdPreGuia(Long idPreGuia);

    @Query("""
            select pp from PreGuiaProcedimento pp
            join fetch pp.procedimentoExame pe
            where pp.preGuia.idPreGuia = :idPreGuia
            order by pe.codigoTuss
            """)
    List<PreGuiaProcedimento> buscarPorPreGuia(@Param("idPreGuia") Long idPreGuia);
}