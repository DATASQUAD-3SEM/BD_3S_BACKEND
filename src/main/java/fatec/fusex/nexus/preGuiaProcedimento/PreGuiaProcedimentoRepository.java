package fatec.fusex.nexus.preGuiaProcedimento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PreGuiaProcedimentoRepository extends JpaRepository<PreGuiaProcedimento, PreGuiaProcedimentoId> {

    List<PreGuiaProcedimento> findByPreGuiaIdPreGuia(Long idPreGuia);
}
