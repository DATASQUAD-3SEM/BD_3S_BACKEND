package fatec.fusex.nexus.guiaRascunho;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade da tabela GuiaRascunhoProcedimento: exame da Guia (Cod DGP, descricao e quantidade)
 * conferido pelo Setor de Guias no rascunho (T22-BE1). Copiado para GuiaProcedimento ao concluir (T22-BE2).
 */
@Entity
@Table(name = "GuiaRascunhoProcedimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuiaRascunhoProcedimento {

    @EmbeddedId
    private GuiaRascunhoProcedimentoId id;

    @Column(name = "cod_dgp", nullable = false, length = 10)
    private String codDgp;

    @Column(name = "descricao_procedimento", nullable = false, length = 500)
    private String descricaoProcedimento;

    @Column(name = "quantidade_autorizada", nullable = false)
    private Integer quantidadeAutorizada;
}
