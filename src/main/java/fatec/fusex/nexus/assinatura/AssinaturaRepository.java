package fatec.fusex.nexus.assinatura;

import org.springframework.data.jpa.repository.JpaRepository;

/*
    Acesso as assinaturas digitais cadastradas pelos usuarios. Cada cadastro gera uma linha nova.
 */
public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
}