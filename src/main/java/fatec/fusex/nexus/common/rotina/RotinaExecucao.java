package fatec.fusex.nexus.common.rotina;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabela RotinaExecucao: uma linha por rotina de execucao unica que ja terminou (T16-BE3).
 */
@Entity
@Table(name = "RotinaExecucao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RotinaExecucao {

    @Id
    @Column(length = 100)
    private String nome;

    @Column(name = "data_execucao", nullable = false)
    private LocalDateTime dataExecucao;
}
