package fatec.fusex.nexus.procedimentoExame;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedimentoExameRepository extends JpaRepository<ProcedimentoExame, Long> {

    List<ProcedimentoExame> findByCodigoTuss(String codigoTuss);

    // O Spring Data cria a query automaticamente lendo o nome do método
    List<ProcedimentoExame> findTop30ByTerminologiaProcedimentoContainingIgnoreCaseOrCodigoTussContainingIgnoreCaseOrderByTerminologiaProcedimentoAsc(
            String termoNome, String termoCodigo);
}
