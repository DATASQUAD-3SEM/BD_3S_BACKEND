package fatec.fusex.nexus.usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import fatec.fusex.nexus.common.exception.MuitasTentativasException;

/**
 * Controla as tentativas erradas de senha atual (T13-BE1): apos 5 falhas seguidas,
 * o usuario fica bloqueado por 15 minutos (429). Um acerto zera a contagem.
 */
@Component
public class TentativasSenhaService {

    private static final int LIMITE_TENTATIVAS = 5;
    private static final Duration DURACAO_BLOQUEIO =
            Duration.ofMinutes(15);

    private final Clock clock;
    private final ConcurrentMap<String, EstadoTentativas> tentativas =
            new ConcurrentHashMap<>();

    public TentativasSenhaService() {
        this(Clock.systemUTC());
    }

    TentativasSenhaService(Clock clock) {
        this.clock = clock;
    }

    public void verificarBloqueio(String login) {
        EstadoTentativas estado = tentativas.get(login);

        if (estado == null || estado.bloqueadoAte() == null) {
            return;
        }

        Instant agora = clock.instant();

        if (agora.isBefore(estado.bloqueadoAte())) {
            throw new MuitasTentativasException();
        }

        tentativas.remove(login, estado);
    }

    public void registrarFalha(String login) {
        Instant agora = clock.instant();

        tentativas.compute(login, (chave, estado) -> {
            if (estado != null
                    && estado.bloqueadoAte() != null
                    && agora.isBefore(estado.bloqueadoAte())) {
                return estado;
            }

            boolean bloqueioExpirado = estado != null
                    && estado.bloqueadoAte() != null;

            int quantidade = estado == null || bloqueioExpirado
                    ? 1
                    : estado.quantidade() + 1;

            Instant bloqueadoAte = quantidade >= LIMITE_TENTATIVAS
                    ? agora.plus(DURACAO_BLOQUEIO)
                    : null;

            return new EstadoTentativas(quantidade, bloqueadoAte);
        });
    }

    public void registrarSucesso(String login) {
        tentativas.remove(login);
    }

    private record EstadoTentativas(
            int quantidade,
            Instant bloqueadoAte) {
    }
}