package fatec.fusex.nexus.setorGuias;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import fatec.fusex.nexus.common.exception.LimiteRequisicoesException;

/**
 * Limita a leitura de PDFs da Guia (T11-BE1) a 20 requisicoes por minuto para cada usuario.
 * A contagem fica em memoria: ao reiniciar o servidor, ela zera.
 */
@Component
public class LimiteLeituraGuia {

    private static final int LIMITE_POR_JANELA = 20;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private final Clock clock;
    private final ConcurrentMap<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimiteLeituraGuia() {
        this(Clock.systemUTC());
    }

    LimiteLeituraGuia(Clock clock) {
        this.clock = clock;
    }

    /** Conta a requisicao do login; lanca 429 quando ele passa do limite da janela atual. */
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
                    "Muitas leituras de Guia em pouco tempo. Aguarde um instante e tente novamente.");
        }
    }

    private record Janela(Instant inicio, int quantidade) {
    }
}
