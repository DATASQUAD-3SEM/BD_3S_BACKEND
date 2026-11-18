package fatec.fusex.nexus.setorGuias;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * T16-BE3: dispara a correcao das Guias antigas na inicializacao. Liga por padrao; a propriedade
 * nexus.guia.correcao-validade.ativa=false desliga (depois que a equipe rodar a correcao em producao).
 */
@Component
@ConditionalOnProperty(name = "nexus.guia.correcao-validade.ativa", havingValue = "true", matchIfMissing = true)
public class CorrecaoValidadeGuiasRunner implements ApplicationRunner {

    private final CorrecaoValidadeGuias correcaoValidadeGuias;

    public CorrecaoValidadeGuiasRunner(CorrecaoValidadeGuias correcaoValidadeGuias) {
        this.correcaoValidadeGuias = correcaoValidadeGuias;
    }

    @Override
    public void run(ApplicationArguments args) {
        correcaoValidadeGuias.executar();
    }
}
