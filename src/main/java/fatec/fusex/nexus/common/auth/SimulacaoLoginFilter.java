package fatec.fusex.nexus.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SimulacaoLoginFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Usuario-Login";

    private final SimulacaoLogin simulacaoLogin;

    public SimulacaoLoginFilter(SimulacaoLogin simulacaoLogin) {
        this.simulacaoLogin = simulacaoLogin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String login = request.getHeader(HEADER);
            if (login != null && !login.isBlank()) {
                simulacaoLogin.definir(login);
            }
            chain.doFilter(request, response);
        } finally {
            simulacaoLogin.limpar();
        }
    }
}