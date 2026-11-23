package fatec.fusex.nexus.dependente;

import java.time.LocalDate;

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

/** Dependente do titular (modelo 2.7). Cadastrado pelo chefe; so os MAIOR tem conta propria. */
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

    /** Conta propria: so para o MAIOR. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Responsavel externo que age pelo menor (MENOR_RESPONSAVEL). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "telefone", length = 11)
    private String telefone;

    @Column(name = "prec_cp", nullable = false, length = 11)
    private String precCp;

    @Column(name = "vinculo", length = 50)
    private String vinculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 17)
    private TipoDependente tipo;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;
}
