package fatec.fusex.nexus.chefeFusex;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados do Chefe do FUSEX.
 */
public interface ChefeFusexRepository
        extends JpaRepository<ChefeFusex, Long> {
}