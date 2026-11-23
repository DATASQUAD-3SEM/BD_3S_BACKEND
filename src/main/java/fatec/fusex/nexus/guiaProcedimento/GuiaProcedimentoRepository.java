package fatec.fusex.nexus.guiaProcedimento;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos exames autorizados das Guias.
 */
public interface GuiaProcedimentoRepository extends JpaRepository<GuiaProcedimento, Long> {

    List<GuiaProcedimento> findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(Collection<Long> guiaIds);
}
