package fatec.fusex.nexus.espelho;

import java.math.BigDecimal;

import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "EspelhoItem")
@Getter
@Setter
@NoArgsConstructor
public class EspelhoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_espelho_item")
    private Long idEspelhoItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espelho_id", nullable = false)
    private Espelho espelho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_procedimento_id", nullable = false, unique = true)
    private GuiaProcedimento guiaProcedimento;

    @Column(name = "quantidade_sessoes", nullable = false)
    private Integer quantidadeSessoes;

    @Column(name = "valor_sessao", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorSessao;

    @Column(name = "valor_contrato_sessao", precision = 10, scale = 2)
    private BigDecimal valorContratoSessao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "origem_contrato", length = 16)
    private OrigemContrato origemContrato;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "decisao_ocs", length = 10)
    private String decisaoOcs;

    @Column(name = "motivo_contestacao_ocs", length = 500)
    private String motivoContestacaoOcs;
}