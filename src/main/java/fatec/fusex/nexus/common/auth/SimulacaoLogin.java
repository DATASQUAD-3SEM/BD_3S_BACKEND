package fatec.fusex.nexus.common.auth;

import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import org.springframework.stereotype.Component;

@Component
public class SimulacaoLogin {

    private final ThreadLocal<String> login = new ThreadLocal<>();

    public void definir(String valor) {
        this.login.set(valor);
    }

    public String loginAtual() {
        String valor = login.get();
        if (valor == null || valor.isBlank()) {
            throw new NaoAutenticadoException(
                    "Nenhum usuario logado. Envie o header 'X-Usuario-Login' (simulacao de login).");
        }
        return valor;
    }

    public void limpar() {
        login.remove();
    }
}