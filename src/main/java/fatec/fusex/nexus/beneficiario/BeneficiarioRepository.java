package fatec.fusex.nexus.beneficiario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long> {

    Optional<Beneficiario> findByCpf(String cpf);

    Optional<Beneficiario> findByPrecCp(String precCp);

    boolean existsByCpf(String cpf);

    boolean existsByPrecCp(String precCp);
}
