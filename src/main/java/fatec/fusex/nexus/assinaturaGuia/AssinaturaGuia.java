package fatec.fusex.nexus.assinaturaGuia;

import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.guia.Guia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "AssinaturaGuia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assinaturaguia_guia_id_papel_unico",
                columnNames = {"guia_id", "papel"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaGuia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_assinatura_guia")
    private Long idAssinaturaGuia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_id", nullable = false)
    private Guia guia;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 6)
    private AssinaturaGuiaPapel papel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_assinatura", nullable = false)
    private LocalDateTime dataAssinatura = LocalDateTime.now();

}