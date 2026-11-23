package fatec.fusex.nexus.andamentoGlosa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import fatec.fusex.nexus.espelho.EspelhoItemRodadaOcs;

/** Leitura das rodadas da OCS de uma fatura, com item, procedimento, Guia e eventos (T26-BE4). */
public interface AndamentoGlosaRepository extends Repository<EspelhoItemRodadaOcs, Long> {

    @Query("""
            SELECT r FROM EspelhoItemRodadaOcs r
            JOIN FETCH r.espelhoItem i
            JOIN FETCH i.guiaProcedimento gp
            JOIN FETCH gp.guia g
            JOIN FETCH r.envioEvento envio
            LEFT JOIN FETCH r.decisaoEvento decisao
            WHERE i.espelho.idEspelho = :faturaId
            ORDER BY envio.dataEvento ASC, envio.idFaturaEvento ASC, r.idRodadaOcs ASC
            """)
    List<EspelhoItemRodadaOcs> rodadasDaFatura(@Param("faturaId") Long faturaId);
}
