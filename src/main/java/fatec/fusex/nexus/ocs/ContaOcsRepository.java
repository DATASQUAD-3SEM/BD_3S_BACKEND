package fatec.fusex.nexus.ocs;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso as contas OCS. A chave e o id do usuario (mesma regra de BeneficiarioRepository).
 */
public interface ContaOcsRepository extends JpaRepository<ContaOcs, Long> {
}
