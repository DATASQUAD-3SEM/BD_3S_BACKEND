package fatec.fusex.nexus.assinatura;

import fatec.fusex.nexus.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Assinatura", uniqueConstraints = {
        @UniqueConstraint(name = "uk_assinatura_usuario_assinatura",
                columnNames = {"usuario_id", "assinatura_url"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Assinatura {
    @Id
    @Column(name="id_assinatura")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAssinatura;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "assinatura_url", nullable = false, length = 500)
    private String assinaturaUrl;

}
