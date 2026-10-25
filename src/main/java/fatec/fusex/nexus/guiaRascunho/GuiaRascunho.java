package fatec.fusex.nexus.guiaRascunho;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.MapsId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade da tabela GuiaRascunho: numero e PDF anexados pelo Setor de Guias antes de clicar em "Concluir" (T4-BE5).
 * O rascunho pode ser trocado quantas vezes for preciso; a Guia definitiva so e criada na conclusao (T4-BE6).
 */
@Entity
@Table(name = "GuiaRascunho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuiaRascunho {

    @Id
    @Column(name = "pre_guia_id")
    private Long preGuiaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "pre_guia_id")
    private PreGuia preGuia;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(name = "arquivo_url", nullable = false, length = 500)
    private String arquivoUrl;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao = LocalDateTime.now();
}