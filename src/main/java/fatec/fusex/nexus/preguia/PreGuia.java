package fatec.fusex.nexus.preguia;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.procedimento.ProcedimentoExame;

/**
 * Entidade responsável pela pré-guia.
 *
 * O encaminhamento médico não é uma entidade:
 * é armazenado como arquivo através da URL/caminho
 * no atributo encaminhamentoUrl.
 */
@Entity
@Table(name = "pre_guia")
public class PreGuia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPreGuia status = StatusPreGuia.RASCUNHO;

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao = LocalDateTime.now();

    @Column(name = "encaminhamento_url", length = 500)
    private String encaminhamentoUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Beneficiario beneficiario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @ManyToMany
    @JoinTable(
            name = "pre_guia_procedimento",
            joinColumns = @JoinColumn(name = "pre_guia_id"),
            inverseJoinColumns = @JoinColumn(name = "procedimento_id")
    )
    private Set<ProcedimentoExame> procedimentos = new HashSet<>();

    public PreGuia() {
    }

    public Long getId() {
        return id;
    }

    public StatusPreGuia getStatus() {
        return status;
    }

    public void setStatus(StatusPreGuia status) {
        this.status = status;
    }

    public LocalDateTime getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDateTime dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getEncaminhamentoUrl() {
        return encaminhamentoUrl;
    }

    public void setEncaminhamentoUrl(String encaminhamentoUrl) {
        this.encaminhamentoUrl = encaminhamentoUrl;
    }

    public Beneficiario getBeneficiario() {
        return beneficiario;
    }

    public void setBeneficiario(Beneficiario beneficiario) {
        this.beneficiario = beneficiario;
    }

    public Ocs getOcs() {
        return ocs;
    }

    public void setOcs(Ocs ocs) {
        this.ocs = ocs;
    }

    public Set<ProcedimentoExame> getProcedimentos() {
        return procedimentos;
    }

    public void setProcedimentos(Set<ProcedimentoExame> procedimentos) {
        this.procedimentos = procedimentos;
    }

    // ==========================================
    // MÉTODOS DE TRANSIÇÃO DE STATUS
    // ==========================================

    //Confirma o envio da pré-guia.

    //RASCUNHO -> PENDENTE

    public void confirmarEnvio() {
        if (status != StatusPreGuia.RASCUNHO) {
            throw new IllegalStateException(
                    "A pré-guia só pode ser enviada quando estiver em RASCUNHO."
            );
        }

        status = StatusPreGuia.PENDENTE;
    }

    //Inicia a análise da pré-guia.

    //PENDENTE -> EM_ANALISE

    public void iniciarAnalise() {
        if (status != StatusPreGuia.PENDENTE) {
            throw new IllegalStateException(
                    "A pré-guia só pode entrar em análise quando estiver PENDENTE."
            );
        }

        status = StatusPreGuia.EM_ANALISE;
    }

    //Aprova a pré-guia.

    //EM_ANALISE -> APROVADA

    public void aprovar() {
        if (status != StatusPreGuia.EM_ANALISE) {
            throw new IllegalStateException(
                    "A pré-guia só pode ser aprovada quando estiver EM_ANALISE."
            );
        }

        status = StatusPreGuia.APROVADA;
    }
}