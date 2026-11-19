package fatec.fusex.nexus.dependente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenteRepository extends JpaRepository<Dependente, Long> {

    /** Dependente que tem conta propria (o proprio usuario, se maior de idade). */
    List<Dependente> findByUsuarioIdUsuario(Long usuarioId);
}
