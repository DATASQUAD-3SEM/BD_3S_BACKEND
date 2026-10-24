package fatec.fusex.nexus.preGuiaHistorico;

import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreGuiaHistoricoRepository extends JpaRepository<PreGuiaHistorico, Long> {


    Optional<PreGuiaHistorico> findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(
            Long idPreGuia,
            PreGuiaStatus status
    );

    // Data em que o encaminhamento atual foi registrado no historico (ex: reenvio da T9-BE4)
    Optional<PreGuiaHistorico> findFirstByPreGuiaIdPreGuiaAndEncaminhamentoUrlOrderByDataHoraDesc(
            Long idPreGuia,
            String encaminhamentoUrl
    );
}