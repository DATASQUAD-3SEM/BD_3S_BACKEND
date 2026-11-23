package fatec.fusex.nexus.sessao;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entidade da tabela Sessao: uma unidade da quantidade autorizada de um exame da Guia (modelo logico, secao 2.4).
 * Criada no envio da Guia a OCS (T18-BE2). Sessao ASSINADA guarda quem assinou, quando e com qual assinatura.
 */
@Entity
@Table(name = "Sessao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sessao_procedimento_numero",
                columnNames = {"guia_procedimento_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sessao")
    private Long idSessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_procedimento_id", nullable = false)
    private GuiaProcedimento guiaProcedimento;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    private SituacaoSessao status = SituacaoSessao.DISPONIVEL;

    @Column(name = "data_solicitacao")
    private LocalDateTime dataSolicitacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitada_por")
    private Usuario solicitadaPor;

    @Column(name = "data_assinatura")
    private LocalDateTime dataAssinatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinado_por")
    private Usuario assinadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id")
    private Assinatura assinatura;
}
