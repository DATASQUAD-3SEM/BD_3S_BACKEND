package fatec.fusex.nexus.sessao;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso as sessoes das Guias. A leitura com lock e usada na virada de validade (T18-BE1).
 */
public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    List<Sessao> findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
            Collection<Long> guiaIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sessao s where s.guiaProcedimento.guia.idGuia = :guiaId")
    List<Sessao> buscarComLockPorGuia(@Param("guiaId") Long guiaId);
}
