package fatec.fusex.nexus.espelho;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EspelhoItemRodadaOcsRepository extends JpaRepository<EspelhoItemRodadaOcs, Long> {

    List<EspelhoItemRodadaOcs> findByEnvioEventoIdFaturaEvento(Long idFaturaEvento);
}
