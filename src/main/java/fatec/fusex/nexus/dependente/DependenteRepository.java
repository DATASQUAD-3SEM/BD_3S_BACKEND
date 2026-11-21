package fatec.fusex.nexus.dependente;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenteRepository extends JpaRepository<Dependente, Long> {

    List<Dependente> findByTitularUsuarioIdAndTipo(Long titularId, TipoDependente tipo);

    List<Dependente> findByResponsavelIdUsuarioAndTipo(Long responsavelId, TipoDependente tipo);

    Optional<Dependente> findByUsuarioIdUsuario(Long usuarioId);

    Optional<Dependente> findByPrecCp(String precCp);

    List<Dependente> findByTitularUsuarioIdOrderByIdDependenteAsc(Long titularId);

    List<Dependente> findByTitularUsuarioIdIn(Collection<Long> titularIds);

    List<Dependente> findByPrecCpStartingWith(String prefixo);
}
