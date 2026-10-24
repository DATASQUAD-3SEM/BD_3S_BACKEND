package fatec.fusex.nexus.assinaturaGuia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaGuiaRepository extends JpaRepository<AssinaturaGuia, Long> {

    long countByGuiaPreGuiaIdPreGuia(Long idPreGuia);
}