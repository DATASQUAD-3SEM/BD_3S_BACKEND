package fatec.fusex.nexus.espelho;

import java.time.LocalDateTime;

import fatec.fusex.nexus.ocs.Ocs;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "Espelho")
@Getter
@Setter
@NoArgsConstructor
public class Espelho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_espelho")
    private Long idEspelho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 13)
    private StatusEspelho status = StatusEspelho.EM_CONSTRUCAO;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por", nullable = false)
    private Usuario criadoPor;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enviado_por")
    private Usuario enviadoPor;
}