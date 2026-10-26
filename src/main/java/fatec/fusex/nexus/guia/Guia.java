package fatec.fusex.nexus.guia;

import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade da tabela Guia. A Guia e emitida fora do Nexus (no SIRE); aqui ficam o numero, o PDF e a data de insercao.
 * Cada pre-guia gera no maximo uma Guia (pre_guia_id UNIQUE). A validade e de dois meses a partir da insercao.
 */
@Entity
@Table(name = "Guia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Guia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_guia")
    private Long idGuia;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pre_guia_id", nullable = false, unique = true)
    private PreGuia preGuia;

    @Column(nullable = false, length = 20, unique = true)
    private String numero;

    @Column(name = "arquivo_url", nullable = false, length = 500)
    private String arquivoUrl;

    @Column(name = "data_insercao", nullable = false)
    private LocalDateTime dataInsercao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inserida_por", nullable = false)
    private Usuario usuario;

    public LocalDate validadeAte() {
        return dataInsercao.toLocalDate().plusMonths(2);
    }
}