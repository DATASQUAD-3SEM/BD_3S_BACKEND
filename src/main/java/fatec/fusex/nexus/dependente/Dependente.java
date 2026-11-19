package fatec.fusex.nexus.dependente;

import java.time.LocalDate;
import java.time.LocalDateTime;

import fatec.fusex.nexus.beneficiario.Beneficiario;
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

/**
 * Entidade da tabela Dependente (T29-BE1): pertence a um titular (Beneficiario). usuario so existe quando o
 * dependente tem conta propria (maior de idade).
 */
@Entity
@Table(name = "Dependente")
@Getter
@Setter
@NoArgsConstructor
public class Dependente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dependente")
    private Long idDependente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titular_id", nullable = false)
    private Beneficiario titular;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(length = 11)
    private String telefone;

    @Column(name = "prec_cp", nullable = false, length = 11)
    private String precCp;

    @Column(length = 100)
    private String vinculo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_vigencia", nullable = false, length = 12)
    private TipoVigencia tipoVigencia;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private SituacaoDependente situacao = SituacaoDependente.ATIVO;

    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    /** Menor de idade na data informada (faz 18 anos no dia do aniversario). */
    public boolean menorEm(LocalDate data) {
        return data.isBefore(dataNascimento.plusYears(18));
    }

    /** Ativo e dentro da vigencia na data informada. */
    public boolean vigenteEm(LocalDate data) {
        if (situacao != SituacaoDependente.ATIVO) {
            return false;
        }
        return tipoVigencia == TipoVigencia.VITALICIA || !vigenciaFim.isBefore(data);
    }
}
