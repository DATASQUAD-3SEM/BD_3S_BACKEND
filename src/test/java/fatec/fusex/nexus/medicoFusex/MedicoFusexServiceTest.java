package fatec.fusex.nexus.medicoFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MedicoFusexServiceTest {

    private MedicoFusexService service;

    @Mock
    private MedicoFusexRepository medicoRepository;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new MedicoFusexService(medicoRepository);
    }

    @Test
    void buscarLogado_quandoMedicoAtivoExiste_retornaDados() {
        MedicoFusex medico = new MedicoFusex();
        medico.setUsuarioId(7L);
        medico.setNome("Carlos Eduardo Lima");
        medico.setPatente("Capitao");
        medico.setCrm("123456-SP");

        // Simula o usuário ativo e com perfil MEDICO (necessário para o validarAcessoMedico)
        fatec.fusex.nexus.usuario.Usuario usuario = new fatec.fusex.nexus.usuario.Usuario();
        usuario.setAtivo(true);
        usuario.setPerfil(fatec.fusex.nexus.usuario.PerfilUsuario.MEDICO);
        medico.setUsuario(usuario);

        when(medicoRepository.findByUsuarioLogin("11144477735"))
                .thenReturn(Optional.of(medico));

        MedicoMeResponse resposta = service.buscarLogado("11144477735");

        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.nome()).isEqualTo("Carlos Eduardo Lima");
    }

    @Test
    void buscarLogado_quandoNaoExiste_lancaAcessoNegado() {
        when(medicoRepository.findByUsuarioLogin("00000000000"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarLogado("00000000000"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Acesso restrito a medicos");
    }
}