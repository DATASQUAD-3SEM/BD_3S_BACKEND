package fatec.fusex.nexus.preGuiaHistorico;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.Usuario;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade da tabela PreGuiaHistorico: uma linha por mudanca de status, com quem agiu e quando.
 * Em ATENCAO a observacao e obrigatoria; contas de setor sao registradas pelo usuario do setor, nunca por pessoa.
 */
@Entity
@Table(name = "PreGuiaHistorico")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PreGuiaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pre_guia_historico")
    private Long idPreGuiaHistorico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pre_guia_id", nullable = false)
    private PreGuia preGuia;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    private PreGuiaStatus status;

    @Column(length = 500)
    private String observacao;

    @Column(name = "encaminhamento_url", length = 500)
    private String encaminhamentoUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();
}