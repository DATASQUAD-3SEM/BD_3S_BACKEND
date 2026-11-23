package fatec.fusex.nexus.guiaProcedimento;

import fatec.fusex.nexus.guia.Guia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Entidade da tabela GuiaProcedimento: exame e quantidade autorizada de uma Guia (modelo logico, secao 2.3).
 * A quantidade autorizada define quantas sessoes a Guia gera. O V.Unit da Guia nao e guardado (T28-BE3, V21).
 */
@Entity
@Table(name = "GuiaProcedimento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_guiaprocedimento_guia_cod",
                columnNames = {"guia_id", "cod_dgp"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuiaProcedimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_guia_procedimento")
    private Long idGuiaProcedimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_id", nullable = false)
    private Guia guia;

    @Column(name = "cod_dgp", nullable = false, length = 10)
    private String codDgp;

    @Column(name = "descricao_procedimento", nullable = false, length = 500)
    private String descricaoProcedimento;

    @Column(name = "quantidade_autorizada", nullable = false)
    private Integer quantidadeAutorizada;
}
