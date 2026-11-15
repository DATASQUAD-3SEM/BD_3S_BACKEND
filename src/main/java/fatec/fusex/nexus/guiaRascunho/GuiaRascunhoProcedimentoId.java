package fatec.fusex.nexus.guiaRascunho;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Chave composta do exame do rascunho: pre-guia e posicao do exame na lista (T22-BE1). */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GuiaRascunhoProcedimentoId implements Serializable {

    @Column(name = "pre_guia_id")
    private Long preGuiaId;

    @Column(name = "ordem")
    private Integer ordem;
}
