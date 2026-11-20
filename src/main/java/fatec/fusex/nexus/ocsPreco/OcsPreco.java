package fatec.fusex.nexus.ocsPreco;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "OcsPreco",
        uniqueConstraints = @UniqueConstraint(name = "uk_ocspreco_ocs_cod", columnNames = {"ocs_id", "cod_dgp"}))
@Getter
@Setter
@NoArgsConstructor
public class OcsPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ocs_preco")
    private Long idOcsPreco;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @Column(name = "cod_dgp", nullable = false, length = 10)
    private String codDgp;

    @Column(name = "valor_sessao", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorSessao;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por")
    private Usuario atualizadoPor;
}