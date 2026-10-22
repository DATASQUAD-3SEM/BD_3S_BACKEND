package fatec.fusex.nexus.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.NaoAutenticadoException;

class SimulacaoLoginTest {

    private final SimulacaoLogin simulacaoLogin = new SimulacaoLogin();

    @AfterEach
    void limpar() {
        simulacaoLogin.limpar();
    }

    @Test
    void loginAtual_quandoDefinido_devolveLogin() {
        simulacaoLogin.definir("52998224725");
        assertThat(simulacaoLogin.loginAtual()).isEqualTo("52998224725");
    }

    @Test
    void loginAtual_quandoNaoDefinido_lancaNaoAutenticado() {
        assertThatThrownBy(() -> simulacaoLogin.loginAtual())
                .isInstanceOf(NaoAutenticadoException.class)
                .hasMessageContaining("X-Usuario-Login");
    }

    @Test
    void loginAtual_quandoVazio_lancaNaoAutenticado() {
        simulacaoLogin.definir("   ");
        assertThatThrownBy(() -> simulacaoLogin.loginAtual())
                .isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void limpar_removeLogin() {
        simulacaoLogin.definir("52998224725");
        simulacaoLogin.limpar();
        assertThatThrownBy(() -> simulacaoLogin.loginAtual())
                .isInstanceOf(NaoAutenticadoException.class);
    }
}