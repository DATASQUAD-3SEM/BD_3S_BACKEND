package fatec.fusex.nexus.ocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;

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
}
