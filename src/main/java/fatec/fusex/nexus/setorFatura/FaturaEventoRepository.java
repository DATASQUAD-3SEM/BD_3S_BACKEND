package fatec.fusex.nexus.setorFatura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FaturaEventoRepository extends JpaRepository<FaturaEvento, Long> {

    List<FaturaEvento> findByEspelhoIdEspelhoOrderByDataEventoAscIdFaturaEventoAsc(Long idEspelho);
}