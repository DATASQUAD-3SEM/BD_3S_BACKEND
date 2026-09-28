package fatec.fusex.nexus.procedimento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedimentoExameRepository extends JpaRepository<ProcedimentoExame, Long> {

    List<ProcedimentoExame> findByCodigoTuss(String codigoTuss);
}
