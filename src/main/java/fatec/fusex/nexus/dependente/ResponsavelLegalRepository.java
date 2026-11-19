package fatec.fusex.nexus.dependente;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponsavelLegalRepository extends JpaRepository<ResponsavelLegal, Long> {

    /** Vinculos de responsavel ativos (data_fim nula) de um usuario. */
    List<ResponsavelLegal> findByUsuarioIdUsuarioAndDataFimIsNull(Long usuarioId);
}
