package fatec.fusex.nexus.ocs;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OcsRepository extends JpaRepository<Ocs, Long> {

    List<Ocs> findByNomeContainingIgnoreCase(String nome);
}