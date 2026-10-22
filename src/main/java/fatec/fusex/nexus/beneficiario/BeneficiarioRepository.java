package fatec.fusex.nexus.beneficiario;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long> {

    @Query("select b from Beneficiario b join fetch b.usuario u where u.login = :cpf")
    Optional<Beneficiario> findByCpf(@Param("cpf") String cpf);

    Optional<Beneficiario> findByPrecCp(String precCp);


    @EntityGraph(attributePaths = "usuario")
    Optional<Beneficiario> findWithUsuarioByPrecCp(String precCp);

    default boolean existsByCpf(String cpf) {
        return findByCpf(cpf).isPresent();
    }

    boolean existsByPrecCp(String precCp);
}
