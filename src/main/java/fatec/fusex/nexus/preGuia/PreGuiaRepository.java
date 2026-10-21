package fatec.fusex.nexus.preGuia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PreGuiaRepository extends JpaRepository<PreGuia, Long> {

    List<PreGuia> findByBeneficiarioUsuarioId(Long usuarioId);

    List<PreGuia> findByStatus(PreGuiaStatus status);
}
