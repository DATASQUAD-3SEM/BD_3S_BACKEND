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

    // T7-BE3: na edicao, so conta duplicidade de OUTRO beneficiario
    boolean existsByPrecCpAndUsuarioIdNot(String precCp, Long usuarioId);

    // T7-BE3: carrega o usuario junto (login, senha e ativo sao alterados na edicao)
    @EntityGraph(attributePaths = "usuario")
    Optional<Beneficiario> findWithUsuarioByUsuarioId(Long usuarioId);


    // T7-BE1: filtro da lista do chefe.
    // busca = nome sem acento e minusculo (com curingas escapados); digitos = so os digitos,
    // quando a busca parece CPF/Prec CP (com ou sem mascara), senao vazio.
    String FILTRO_LISTA_CHEFE = """
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
                   LIKE CONCAT('%', :busca, '%') ESCAPE '!' OR
                   (:digitos <> '' AND (u.login LIKE CONCAT('%', :digitos, '%') OR
                                        b.precCp LIKE CONCAT('%', :digitos, '%'))))
              AND (:ativo IS NULL OR u.ativo = :ativo)
            """;

    // JOIN FETCH traz o usuario junto (login e ativo), sem uma consulta extra por item
    @Query(value = "SELECT b FROM Beneficiario b JOIN FETCH b.usuario u " + FILTRO_LISTA_CHEFE,
            countQuery = "SELECT COUNT(b) FROM Beneficiario b JOIN b.usuario u " + FILTRO_LISTA_CHEFE)
    Page<Beneficiario> buscarComFiltros(
            @Param("busca") String busca,
            @Param("digitos") String digitos,
            @Param("ativo") Boolean ativo,
            Pageable pageable);
}