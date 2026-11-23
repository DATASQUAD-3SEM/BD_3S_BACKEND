package fatec.fusex.nexus.dependente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsavel externo (modelo 2.7): pessoa sem Prec CP, com conta propria pelo CPF. */
@Entity
@Table(name = "ResponsavelExterno")
@Getter
@Setter
@NoArgsConstructor
public class ResponsavelExterno {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "telefone", length = 11)
    private String telefone;
}
