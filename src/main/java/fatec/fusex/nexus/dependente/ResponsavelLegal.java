package fatec.fusex.nexus.dependente;

import java.time.LocalDate;

import fatec.fusex.nexus.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Entidade da tabela ResponsavelLegal (T29-BE1). Aponta para Usuario, nunca para Dependente: o vinculo segue
 * mesmo se o dependente deixar de estar ativo. data_fim nula = vinculo ativo.
 */
@Entity
@Table(name = "ResponsavelLegal")
@Getter
@Setter
@NoArgsConstructor
public class ResponsavelLegal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_responsavel_legal")
    private Long idResponsavelLegal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependente_id", nullable = false)
    private Dependente dependente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;
}
