package fatec.fusex.nexus.ocsProcedimento;

import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "OcsProcedimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcsProcedimento {

    @EmbeddedId
    private OcsProcedimentoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("ocsId")
    @JoinColumn(name = "ocs_id")
    private Ocs ocs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("procedimentoId")
    @JoinColumn(name = "procedimento_id")
    private ProcedimentoExame procedimentoExame;
}