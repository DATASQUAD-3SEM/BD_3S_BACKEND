package fatec.fusex.nexus.ocs;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "Ocs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ocs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ocs")
    private Long idOcs;

    @Column(nullable = false, length = 200)
    private String nome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 3)
    private OcsTipo tipo;

    @Column(name = "contrato_num", nullable = false, length = 50)
    private String contratoNum;

    @Column(nullable = false)
    private Boolean ativo = true;
}
