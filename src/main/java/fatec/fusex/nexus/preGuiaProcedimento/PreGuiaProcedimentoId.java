package fatec.fusex.nexus.preGuiaProcedimento;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chave composta de PreGuiaProcedimento (pre_guia_id + procedimento_id).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreGuiaProcedimentoId implements Serializable {

    @Column(name = "pre_guia_id")
    private Long preGuiaId;

    @Column(name = "procedimento_id")
    private Long procedimentoId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreGuiaProcedimentoId that)) return false;
        return Objects.equals(preGuiaId, that.preGuiaId)
                && Objects.equals(procedimentoId, that.procedimentoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preGuiaId, procedimentoId);
    }
}