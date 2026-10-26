package fatec.fusex.nexus.guiaRascunho;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos rascunhos de Guia, identificados pelo id da pre-guia.
 */
public interface GuiaRascunhoRepository extends JpaRepository<GuiaRascunho, Long> {
}