package fatec.fusex.nexus.preGuiaHistorico;

import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acesso ao historico das pre-guias. A observacao exibida nas telas e sempre a mais recente.
 */
@Repository
public interface PreGuiaHistoricoRepository extends JpaRepository<PreGuiaHistorico, Long> {


    Optional<PreGuiaHistorico> findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(
            Long idPreGuia,
            PreGuiaStatus status
    );

    Optional<PreGuiaHistorico> findFirstByPreGuiaIdPreGuiaAndEncaminhamentoUrlAndStatusNotOrderByDataHoraDesc(
            Long idPreGuia,
            String encaminhamentoUrl,
            PreGuiaStatus status
    );
}