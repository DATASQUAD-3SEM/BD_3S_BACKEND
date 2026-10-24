package fatec.fusex.nexus.guia;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GuiaRepository extends JpaRepository<Guia, Long> {

    // Busca a Guia atrelada a uma Pré-Guia específica
    Optional<Guia> findByPreGuiaIdPreGuia(Long idPreGuia);
}