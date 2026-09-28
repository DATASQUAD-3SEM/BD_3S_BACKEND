package fatec.fusex.nexus.preguia;

import java.time.LocalDateTime;

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

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.ocs.Ocs;

/**
 * Entidade responsavel pela pre-guia.
 *
 * O encaminhamento medico nao e uma entidade: e armazenado como arquivo atraves
 * da URL/caminho no atributo encaminhamentoUrl.
 *
 * A pre-guia nao guarda procedimentos: o fluxo nao seleciona exames.
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

    public PreGuia() {
    }

    public Long getId() { return id; }

    public StatusPreGuia getStatus() { return status; }
    public void setStatus(StatusPreGuia status) { this.status = status; }

    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }

    public String getEncaminhamentoUrl() { return encaminhamentoUrl; }
    public void setEncaminhamentoUrl(String encaminhamentoUrl) { this.encaminhamentoUrl = encaminhamentoUrl; }

    public Beneficiario getBeneficiario() { return beneficiario; }
    public void setBeneficiario(Beneficiario beneficiario) { this.beneficiario = beneficiario; }

    public Ocs getOcs() { return ocs; }
    public void setOcs(Ocs ocs) { this.ocs = ocs; }

    public void confirmarEnvio() {
        if (status != StatusPreGuia.RASCUNHO) {
            throw new IllegalStateException("A pré-guia só pode ser enviada quando estiver em RASCUNHO.");
        }
        status = StatusPreGuia.PENDENTE;
    }

    public void iniciarAnalise() {
        if (status != StatusPreGuia.PENDENTE) {
            throw new IllegalStateException("A pré-guia só pode entrar em análise quando estiver PENDENTE.");
        }
        status = StatusPreGuia.EM_ANALISE;
    }

    public void aprovar() {
        if (status != StatusPreGuia.EM_ANALISE) {
            throw new IllegalStateException("A pré-guia só pode ser aprovada quando estiver EM_ANALISE.");
        }
        status = StatusPreGuia.APROVADA;
    }
}