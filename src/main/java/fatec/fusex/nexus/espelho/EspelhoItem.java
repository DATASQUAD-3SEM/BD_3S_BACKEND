package fatec.fusex.nexus.espelho;

import java.math.BigDecimal;

import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}