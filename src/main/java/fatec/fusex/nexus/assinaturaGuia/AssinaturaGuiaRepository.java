package fatec.fusex.nexus.assinaturaGuia;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaGuiaRepository extends JpaRepository<AssinaturaGuia, Long> {

    long countByGuiaPreGuiaIdPreGuia(Long idPreGuia);

    List<AssinaturaGuia> findByGuiaIdGuiaIn(Collection<Long> guiaIds);
}