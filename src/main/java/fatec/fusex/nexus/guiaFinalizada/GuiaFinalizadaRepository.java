package fatec.fusex.nexus.guiaFinalizada;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.SituacaoExecucao;

public interface GuiaFinalizadaRepository extends JpaRepository<Guia, Long> {

    @Query("""
            select g from Guia g
            join fetch g.preGuia pg
            join fetch pg.beneficiario
            left join fetch g.espelho
            where pg.ocs.idOcs = :idOcs
              and g.dataEnvio is not null
              and g.situacaoExecucao in :situacoes
            order by g.dataEnvio desc, g.idGuia desc
            """)
    List<Guia> buscarFinalizadasDaOcs(@Param("idOcs") Long idOcs,
                                      @Param("situacoes") List<SituacaoExecucao> situacoes);
}