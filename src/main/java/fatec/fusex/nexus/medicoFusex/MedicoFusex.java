package fatec.fusex.nexus.medicoFusex;

import fatec.fusex.nexus.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade da tabela MedicoFusex, especializacao 1:1 de Usuario. O CPF do medico e o login do usuario.
 */
@Entity
@Table(name = "MedicoFusex")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicoFusex {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "prec_cp", nullable = false, length = 11, unique = true)
    private String precCp;

    @Column(nullable = false, length = 50)
    private String patente;

    @Column(nullable = false, length = 20, unique = true)
    private String crm;
}