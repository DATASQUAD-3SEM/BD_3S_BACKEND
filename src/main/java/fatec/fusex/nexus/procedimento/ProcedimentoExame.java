package fatec.fusex.nexus.procedimento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabela procedimento_exame (catalogo TUSS/ANS). */
@Entity
@Table(name = "procedimento_exame")
public class ProcedimentoExame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_tuss", nullable = false, length = 20)
    private String codigoTuss;

    @Column(name = "terminologia_procedimento_evento", nullable = false, length = 500)
    private String terminologiaProcedimentoEvento;

    @Column(name = "rol_ans_resolucao_normativa", length = 100)
    private String rolAnsResolucaoNormativa;

    @Column(name = "rol_ans", length = 100)
    private String rolAns;

    private String grupo;
    private String subgrupo;
    private String capitulo;

    public ProcedimentoExame() {
    }

    public Long getId() { return id; }

    public String getCodigoTuss() { return codigoTuss; }
    public void setCodigoTuss(String codigoTuss) { this.codigoTuss = codigoTuss; }

    public String getTerminologiaProcedimentoEvento() { return terminologiaProcedimentoEvento; }
    public void setTerminologiaProcedimentoEvento(String v) { this.terminologiaProcedimentoEvento = v; }

    public String getRolAnsResolucaoNormativa() { return rolAnsResolucaoNormativa; }
    public void setRolAnsResolucaoNormativa(String v) { this.rolAnsResolucaoNormativa = v; }

    public String getRolAns() { return rolAns; }
    public void setRolAns(String rolAns) { this.rolAns = rolAns; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public String getSubgrupo() { return subgrupo; }
    public void setSubgrupo(String subgrupo) { this.subgrupo = subgrupo; }

    public String getCapitulo() { return capitulo; }
    public void setCapitulo(String capitulo) { this.capitulo = capitulo; }
}
