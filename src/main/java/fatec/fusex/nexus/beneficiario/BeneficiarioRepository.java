package fatec.fusex.nexus.beneficiario;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    @Query("""
            SELECT b FROM Beneficiario b JOIN b.usuario u
            WHERE (:busca = '' OR 
                   REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                   REPLACE(REPLACE(REPLACE(
                     LOWER(b.nome),
                     'á','a'),'à','a'),'â','a'),
                     'ã','a'),'ä','a'),
                     'é','e'),'è','e'),'ê','e'),'ë','e'),
                     'í','i'),'ì','i'),'î','i'),'ï','i'),
                     'ó','o'),'ò','o'),'ô','o'),'õ','o'),'ö','o'),
                     'ú','u'),'ù','u'),'û','u'),'ü','u'),
                     'ç','c')
                   LIKE CONCAT('%', :busca, '%') OR 
                   u.login LIKE CONCAT('%', :busca, '%') OR 
                   b.precCp LIKE CONCAT('%', :busca, '%'))
              AND (:ativo IS NULL OR u.ativo = :ativo)
            """)
    Page<Beneficiario> buscarComFiltros(
            @Param("busca") String busca,
            @Param("ativo") Boolean ativo,
            Pageable pageable);
}