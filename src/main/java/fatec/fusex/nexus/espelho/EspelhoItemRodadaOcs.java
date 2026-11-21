package fatec.fusex.nexus.espelho;

import java.math.BigDecimal;

import fatec.fusex.nexus.setorFatura.FaturaEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Historico das rodadas da OCS: uma linha por EspelhoItem enviado em cada ENVIADA_OCS
 * (andamento da glosa, auditoria e verso da Guia).
 */
@Entity
@Table(name = "EspelhoItemRodadaOcs",
        uniqueConstraints = @UniqueConstraint(name = "uk_rodadaocs_item_envio",
                columnNames = {"espelho_item_id", "envio_evento_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EspelhoItemRodadaOcs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rodada_ocs")
    private Long idRodadaOcs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espelho_item_id", nullable = false)
    private EspelhoItem espelhoItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envio_evento_id", nullable = false)
    private FaturaEvento envioEvento;

    @Column(name = "valor_contrato_sessao", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorContratoSessao;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_contrato", nullable = false, length = 16)
    private OrigemContrato origemContrato;

    @Column(name = "decisao_ocs", length = 10)
    private String decisaoOcs;

    @Column(name = "motivo_contestacao_ocs", length = 500)
    private String motivoContestacaoOcs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decisao_evento_id")
    private FaturaEvento decisaoEvento;
}
