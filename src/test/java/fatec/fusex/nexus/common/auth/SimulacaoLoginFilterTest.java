package fatec.fusex.nexus.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import fatec.fusex.nexus.common.exception.NaoAutenticadoException;

class SimulacaoLoginFilterTest {

    private final SimulacaoLogin simulacaoLogin = new SimulacaoLogin();
    private final SimulacaoLoginFilter filtro = new SimulacaoLoginFilter(simulacaoLogin);

    @AfterEach
    void limpar() {
        simulacaoLogin.limpar();
    }

    @Test
    void comHeader_defineLoginDuranteACadeiaELimpaNoFinal() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(SimulacaoLoginFilter.HEADER, "52998224725");
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicReference<String> capturado = new AtomicReference<>();
        FilterChain chain = (request, response) -> capturado.set(simulacaoLogin.loginAtual());

        filtro.doFilter(req, res, chain);

        assertThat(capturado.get()).isEqualTo("52998224725");
        assertThatThrownBy(() -> simulacaoLogin.loginAtual())
                .isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void semHeader_segueACadeiaSemLogin() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        AtomicBoolean chamou = new AtomicBoolean(false);
        FilterChain chain = (request, response) -> {
            chamou.set(true);
            assertThatThrownBy(() -> simulacaoLogin.loginAtual())
                    .isInstanceOf(NaoAutenticadoException.class);
        };

        filtro.doFilter(req, res, chain);

        assertThat(chamou).isTrue();
    }
}