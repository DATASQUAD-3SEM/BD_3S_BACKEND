package fatec.fusex.nexus.ocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.ocs.dto.OcsBuscaResponse;

/**
 * MODELO DE TESTE COM MOCKITO (copie este jeito):
 *  - @Mock          = dependencia FALSA (o repository nao vai ao banco de verdade)
 *  - @InjectMocks   = a classe que estamos testando, ja recebendo os mocks
 *  - when(...).thenReturn(...) = "quando chamarem X, responda Y"
 */
@ExtendWith(MockitoExtension.class)
class OcsServiceTest {

    @Mock
    private OcsRepository ocsRepository;

    @InjectMocks
    private OcsService ocsService;

    @Test
    void buscarPorId_quandoExiste_devolveOcs() {
        Ocs ocs = new Ocs();
        ocs.setNome("Laboratorio X");
        when(ocsRepository.findById(1L)).thenReturn(Optional.of(ocs));

        Ocs resultado = ocsService.buscarPorId(1L);

        assertThat(resultado.getNome()).isEqualTo("Laboratorio X");
    }

    @Test
    void buscarPorId_quandoNaoExiste_lancaExcecao() {
        when(ocsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ocsService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // ===== buscar() - T2-BE2 =====

    private Ocs ocs(long id, String nome, String contrato, OcsTipo tipo) {
        Ocs ocs = new Ocs();
        ocs.setIdOcs(id);
        ocs.setNome(nome);
        ocs.setContratoNum(contrato);
        ocs.setTipo(tipo);
        return ocs;
    }

    @Test
    void buscar_semTermo_listaAtivasComLimite30OrdenadasPorNome() {
        Page<Ocs> pagina = new PageImpl<>(List.of(ocs(1L, "Hospital Y", "CTR-002", OcsTipo.PSA)));
        when(ocsRepository.findByAtivoTrue(any(Pageable.class))).thenReturn(pagina);

        OcsBuscaResponse resposta = ocsService.buscar("   ");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ocsRepository).findByAtivoTrue(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
        assertThat(captor.getValue().getSort().getOrderFor("nome").isAscending()).isTrue();
        verify(ocsRepository, never()).buscarPorTermo(anyString(), any(Pageable.class));

        assertThat(resposta.total()).isEqualTo(1L);
        assertThat(resposta.resultados().get(0).contrato()).isEqualTo("CTR-002");
        assertThat(resposta.resultados().get(0).tipo()).isEqualTo("PSA");
    }

    @Test
    void buscar_comTermo_removeEspacosDasPontas() {
        when(ocsRepository.buscarPorTermo(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ocsService.buscar("  lab  ");

        verify(ocsRepository).buscarPorTermo(org.mockito.ArgumentMatchers.eq("lab"), any(Pageable.class));
    }

    @Test
    void buscar_totalMaiorQueOLimite_devolve30ItensComTotalGeral() {
        List<Ocs> trinta = IntStream.rangeClosed(1, 30)
                .mapToObj(i -> ocs(i, "OCS " + i, "CTR-" + i, OcsTipo.OCS))
                .toList();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 30);
        when(ocsRepository.findByAtivoTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(trinta, pageable, 35));

        OcsBuscaResponse resposta = ocsService.buscar(null);

        assertThat(resposta.resultados()).hasSize(30);
        assertThat(resposta.total()).isEqualTo(35L);
    }

    @Test
    void buscar_curingasDoLike_valemComoTextoLiteral() {
        when(ocsRepository.buscarPorTermo(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ocsService.buscar("100%_a!");

        verify(ocsRepository).buscarPorTermo(org.mockito.ArgumentMatchers.eq("100!%!_a!!"), any(Pageable.class));
    }
}
