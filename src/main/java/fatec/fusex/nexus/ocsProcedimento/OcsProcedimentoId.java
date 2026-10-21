package fatec.fusex.nexus.ocsProcedimento;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcsProcedimentoId implements Serializable {

    @Column(name = "ocs_id")
    private Long ocsId;

    @Column(name = "procedimento_id")
    private Long procedimentoId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OcsProcedimentoId that)) return false;
        return Objects.equals(ocsId, that.ocsId)
                && Objects.equals(procedimentoId, that.procedimentoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ocsId, procedimentoId);
    }
}