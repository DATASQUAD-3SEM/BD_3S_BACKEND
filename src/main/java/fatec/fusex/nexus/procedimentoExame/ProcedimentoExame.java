package fatec.fusex.nexus.procedimentoExame;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade da tabela ProcedimentoExame: catalogo de procedimentos e exames, identificados pelo codigo TUSS.
 */
@Entity
@Table(name = "ProcedimentoExame")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcedimentoExame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_procedimento_exame")
    private Long idProcedimentoExame;

    @Column(name = "codigo_tuss", nullable = false, length = 10, unique = true)
    private String codigoTuss;

    @Column(name = "terminologia_procedimento", nullable = false, length = 500)
    private String terminologiaProcedimento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rol_ans", nullable = false, length = 3)
    private ProcedimentoExameRolAns rolAns;

    @Column(name = "rol_ans_resolucao_normativa", length = 100)
    private String rolAnsResolucaoNormativa;

    @Column(length = 150)
    private String subgrupo;
}
