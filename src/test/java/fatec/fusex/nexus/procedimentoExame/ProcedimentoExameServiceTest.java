package fatec.fusex.nexus.procedimentoExame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;

/**
 * Testes da busca no catalogo de procedimentos (T2-BE3): termo curto, limite e ordenacao.
 */
@ExtendWith(MockitoExtension.class)
class ProcedimentoExameServiceTest {

    @Mock
    private ProcedimentoExameRepository procedimentoRepository;

    @InjectMocks
    private ProcedimentoExameService procedimentoService;

    private ProcedimentoExame procedimento(String codigoTuss, String nome) {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss(codigoTuss);
        p.setTerminologiaProcedimento(nome);
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        return p;
    }

    @Test
    void buscar_comTermoValido_devolveCodigoTussENomeComLimite30OrdenadoPorNome() {
        when(procedimentoRepository.buscarPorTermo(anyString(), any(Pageable.class)))
                .thenReturn(List.of(procedimento("40302040", "Glicose")));

        List<ProcedimentoMedicoResponse> resposta = procedimentoService.buscar("glic");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(procedimentoRepository).buscarPorTermo(eq("glic"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
        assertThat(captor.getValue().getSort().getOrderFor("terminologiaProcedimento").isAscending()).isTrue();

        assertThat(resposta).containsExactly(new ProcedimentoMedicoResponse("40302040", "Glicose"));
    }

    @Test
    void buscar_comTermo_removeEspacosDasPontas() {
        when(procedimentoRepository.buscarPorTermo(anyString(), any(Pageable.class))).thenReturn(List.of());

        procedimentoService.buscar("  4030  ");

        verify(procedimentoRepository).buscarPorTermo(eq("4030"), any(Pageable.class));
    }

    @Test
    void buscar_semResultado_devolveListaVazia() {
        when(procedimentoRepository.buscarPorTermo(anyString(), any(Pageable.class))).thenReturn(List.of());

        assertThat(procedimentoService.buscar("inexistente")).isEmpty();
    }

    @Test
    void buscar_termoComMenosDe2Caracteres_lancaRegraDeNegocioSemConsultar() {
        assertThatThrownBy(() -> procedimentoService.buscar("a"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("2 caracteres");
        // espacos nao contam: " a " continua curto
        assertThatThrownBy(() -> procedimentoService.buscar(" a "))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(procedimentoRepository, never()).buscarPorTermo(anyString(), any(Pageable.class));
    }

    @Test
    void buscar_semTermo_lancaRegraDeNegocio() {
        assertThatThrownBy(() -> procedimentoService.buscar(null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> procedimentoService.buscar("   "))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void buscar_curingasDoLike_valemComoTextoLiteral() {
        when(procedimentoRepository.buscarPorTermo(anyString(), any(Pageable.class))).thenReturn(List.of());

        procedimentoService.buscar("100%_a!");

        verify(procedimentoRepository).buscarPorTermo(eq("100!%!_a!!"), any(Pageable.class));
    }
}
