package fatec.fusex.nexus.ocs;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import fatec.fusex.nexus.procedimento.ProcedimentoExame;

/** Tabela ocs (rede credenciada). A tabela ocs_procedimento e mapeada aqui pelo @ManyToMany. */
@Entity
@Table(name = "ocs")
public class Ocs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrato_num", nullable = false, length = 50)
    private String contratoNum;

    @Column(nullable = false)
    private String nome;

    @Column(name = "inicio_vigencia")
    private LocalDate inicioVigencia;

    @Column(length = 50)
    private String tipo;

    @Column(name = "dias_para_vencimento")
    private Integer diasParaVencimento;

    @Column(name = "termino_vigencia")
    private LocalDate terminoVigencia;

    @ManyToMany
    @JoinTable(
            name = "ocs_procedimento",
            joinColumns = @JoinColumn(name = "ocs_id"),
            inverseJoinColumns = @JoinColumn(name = "procedimento_id"))
    private Set<ProcedimentoExame> procedimentos = new HashSet<>();

    public Ocs() {
    }

    public Long getId() { return id; }

    public String getContratoNum() { return contratoNum; }
    public void setContratoNum(String contratoNum) { this.contratoNum = contratoNum; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public LocalDate getInicioVigencia() { return inicioVigencia; }
    public void setInicioVigencia(LocalDate inicioVigencia) { this.inicioVigencia = inicioVigencia; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getDiasParaVencimento() { return diasParaVencimento; }
    public void setDiasParaVencimento(Integer diasParaVencimento) { this.diasParaVencimento = diasParaVencimento; }

    public LocalDate getTerminoVigencia() { return terminoVigencia; }
    public void setTerminoVigencia(LocalDate terminoVigencia) { this.terminoVigencia = terminoVigencia; }

    public Set<ProcedimentoExame> getProcedimentos() { return procedimentos; }
    public void setProcedimentos(Set<ProcedimentoExame> procedimentos) { this.procedimentos = procedimentos; }
}
