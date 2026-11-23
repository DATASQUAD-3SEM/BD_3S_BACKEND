package fatec.fusex.nexus.ocs;

import fatec.fusex.nexus.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade da tabela ContaOcs, especializacao 1:1 de Usuario (o id e o mesmo do usuario).
 * Liga a conta do perfil OCS a uma unica linha de Ocs; ocs_id e unico, entao uma conta por OCS.
 */
@Entity
@Table(name = "ContaOcs")
@Getter
@Setter
@NoArgsConstructor
public class ContaOcs {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ocs_id", nullable = false, unique = true)
    private Ocs ocs;
}
