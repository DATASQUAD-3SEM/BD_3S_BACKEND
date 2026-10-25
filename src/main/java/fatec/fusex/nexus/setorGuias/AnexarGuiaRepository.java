package fatec.fusex.nexus.setorGuias;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.preGuia.PreGuia;
import jakarta.persistence.LockModeType;

/** T4-BE5: repositorio proprio do anexo da Guia (rascunho), com as consultas que a task precisa. */
public interface AnexarGuiaRepository extends JpaRepository<GuiaRascunho, Long> {

    // Trava a pre-guia: dois anexos ao mesmo tempo nao gravam rascunhos misturados
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreGuia p WHERE p.idPreGuia = :id")
    Optional<PreGuia> buscarPreGuiaParaAnexar(@Param("id") Long id);

    // A pre-guia ja tem a Guia concluida (guia.pre_guia_id e unico)
    @Query("SELECT COUNT(g) > 0 FROM Guia g WHERE g.preGuia.idPreGuia = :idPreGuia")
    boolean temGuiaConcluida(@Param("idPreGuia") Long idPreGuia);

    // Numero ja usado em outra Guia
    @Query("SELECT COUNT(g) > 0 FROM Guia g WHERE g.numero = :numero")
    boolean numeroUsadoEmGuia(@Param("numero") String numero);

    // Numero ja reservado no rascunho de OUTRA pre-guia
    @Query("SELECT COUNT(r) > 0 FROM GuiaRascunho r WHERE r.numero = :numero AND r.preGuiaId <> :idPreGuia")
    boolean numeroUsadoEmOutroRascunho(@Param("numero") String numero, @Param("idPreGuia") Long idPreGuia);
}
