package fatec.fusex.nexus.medicoFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MedicoFusexServiceTest {

    private MedicoFusexService service;

    @Mock
    private ProcedimentoExameRepository repository;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new MedicoFusexService(repository);
    }

    @Test
    void buscar_quandoTermoMenorQue2_lancaRegraDeNegocioException() {
        assertThatThrownBy(() -> service.buscar("a"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("no mínimo 2 caracteres");

        assertThatThrownBy(() -> service.buscar(null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void buscar_quandoTermoValido_retornaListaMapeada() {
        ProcedimentoExame exame = new ProcedimentoExame();
        exame.setCodigoTuss("40304361");
        exame.setTerminologiaProcedimento("Hemograma completo");

        when(repository.findTop30ByTerminologiaProcedimentoContainingIgnoreCaseOrCodigoTussContainingIgnoreCaseOrderByTerminologiaProcedimentoAsc(anyString(), anyString()))
                .thenReturn(List.of(exame));

        List<ProcedimentoMedicoResponse> resultado = service.buscar("hemo");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).codigoTuss()).isEqualTo("40304361");
        assertThat(resultado.get(0).nome()).isEqualTo("Hemograma completo");
        verify(repository).findTop30ByTerminologiaProcedimentoContainingIgnoreCaseOrCodigoTussContainingIgnoreCaseOrderByTerminologiaProcedimentoAsc("hemo", "hemo");
    }
}