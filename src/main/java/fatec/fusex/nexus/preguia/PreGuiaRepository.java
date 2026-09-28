package fatec.fusex.nexus.preguia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PreGuiaRepository extends JpaRepository<PreGuia, Long> {

    List<PreGuia> findByBeneficiarioId(Long beneficiarioId);

    List<PreGuia> findByStatus(StatusPreGuia status);
}
