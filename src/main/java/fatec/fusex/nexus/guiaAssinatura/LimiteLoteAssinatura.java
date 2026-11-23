package fatec.fusex.nexus.guiaAssinatura;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import fatec.fusex.nexus.common.exception.LimiteRequisicoesException;

@Component
public class LimiteLoteAssinatura {

    private static final int LIMITE_POR_JANELA = 10;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private final Clock clock;
    private final ConcurrentMap<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimiteLoteAssinatura() {
        this(Clock.systemUTC());
    }

    LimiteLoteAssinatura(Clock clock) {
        this.clock = clock;
    }

    // Conta a requisicao na janela do login; lanca 429 ao passar de 10 por minuto
    public void registrar(String login) {
        Instant agora = clock.instant();

        Janela janela = janelas.compute(login, (chave, atual) -> {
            if (atual == null || !agora.isBefore(atual.inicio().plus(JANELA))) {
                return new Janela(agora, 1);
            }
            return new Janela(atual.inicio(), atual.quantidade() + 1);
        });

        if (janela.quantidade() > LIMITE_POR_JANELA) {
            throw new LimiteRequisicoesException(
                    "Muitos lotes de assinatura em pouco tempo. Aguarde um instante e tente novamente.");
        }
    }

    private record Janela(Instant inicio, int quantidade) {
    }
}
