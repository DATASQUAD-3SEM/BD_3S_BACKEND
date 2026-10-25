package fatec.fusex.nexus.preGuia;

import java.time.LocalDateTime;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.ocs.Ocs;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade da tabela PreGuia. medico_id nulo indica pre-guia criada pelo beneficiario (com encaminhamento);
 * preenchido indica pre-guia criada pelo medico (sem encaminhamento e com lista de exames).
 */
@Entity
@Table(name = "PreGuia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreGuia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pre_guia")
    private Long idPreGuia;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    private PreGuiaStatus status = PreGuiaStatus.PENDENTE;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "encaminhamento_url", length = 500)
    private String encaminhamentoUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Beneficiario beneficiario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private MedicoFusex medicoFusex;
}