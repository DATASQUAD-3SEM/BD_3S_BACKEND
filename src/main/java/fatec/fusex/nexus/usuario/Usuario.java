package fatec.fusex.nexus.usuario;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade da tabela Usuario, base de todas as contas. O login e o CPF (so digitos) para beneficiario e medico,
 * e o nome de usuario para chefe e setores. assinatura_url fica nula ate o proprio usuario cadastrar a assinatura.
 */
@Entity
@Table(name = "Usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(nullable = false, length = 50, unique = true)
    private String login;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 15)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "assinatura_url", length = 500)
    private String assinaturaUrl;

}


