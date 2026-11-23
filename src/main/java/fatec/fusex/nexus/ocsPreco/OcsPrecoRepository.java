package fatec.fusex.nexus.ocsPreco;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OcsPrecoRepository extends JpaRepository<OcsPreco, Long> {

    Optional<OcsPreco> findByOcsIdOcsAndCodDgp(Long idOcs, String codDgp);
}