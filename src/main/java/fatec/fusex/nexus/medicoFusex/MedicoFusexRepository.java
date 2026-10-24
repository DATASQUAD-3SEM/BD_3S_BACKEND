package fatec.fusex.nexus.medicoFusex;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicoFusexRepository extends JpaRepository<MedicoFusex, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<MedicoFusex> findByUsuarioLogin(String login);

    @EntityGraph(attributePaths = "usuario")
    Optional<MedicoFusex> findByUsuarioLoginAndUsuarioAtivoTrue(String login);

    // T8-BE2: duplicidade no cadastro pelo chefe
    boolean existsByPrecCp(String precCp);

    boolean existsByCrm(String crm);

    // T8-BE3: na edicao, so conta duplicidade de OUTRO medico
    boolean existsByPrecCpAndUsuarioIdNot(String precCp, Long usuarioId);

    boolean existsByCrmAndUsuarioIdNot(String crm, Long usuarioId);

    // T8-BE3: carrega o usuario junto (login, senha e ativo sao alterados na edicao)
    @EntityGraph(attributePaths = "usuario")
    Optional<MedicoFusex> findWithUsuarioByUsuarioId(Long usuarioId);

    // T8-BE1: lista do chefe. ativo nulo = todos; digitos vazio = a busca nao parece CPF/Prec CP
    @Query(value = """
            SELECT m FROM MedicoFusex m JOIN FETCH m.usuario u
            WHERE (:ativo IS NULL OR u.ativo = :ativo)
              AND (:busca = ''
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
                   OR LOWER(m.crm) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
                   OR (:digitos <> '' AND (u.login LIKE CONCAT('%', :digitos, '%')
                                           OR m.precCp LIKE CONCAT('%', :digitos, '%'))))
            """,
            countQuery = """
            SELECT COUNT(m) FROM MedicoFusex m JOIN m.usuario u
            WHERE (:ativo IS NULL OR u.ativo = :ativo)
              AND (:busca = ''
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
                   OR LOWER(m.crm) LIKE LOWER(CONCAT('%', :busca, '%')) ESCAPE '!'
                   OR (:digitos <> '' AND (u.login LIKE CONCAT('%', :digitos, '%')
                                           OR m.precCp LIKE CONCAT('%', :digitos, '%'))))
            """)
    Page<MedicoFusex> buscarParaChefe(
            @Param("ativo") Boolean ativo,
            @Param("busca") String busca,
            @Param("digitos") String digitos,
            Pageable pageable);
}
