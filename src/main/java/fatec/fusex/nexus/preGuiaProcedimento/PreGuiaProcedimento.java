package fatec.fusex.nexus.preGuiaProcedimento;

import fatec.fusex.nexus.preGuia.PreGuia;
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
@Table(name = "PreGuiaProcedimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreGuiaProcedimento {

    @EmbeddedId
    private PreGuiaProcedimentoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("preGuiaId")
    @JoinColumn(name = "pre_guia_id")
    private PreGuia preGuia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("procedimentoId")
    @JoinColumn(name = "procedimento_id")
    private ProcedimentoExame procedimentoExame;
}