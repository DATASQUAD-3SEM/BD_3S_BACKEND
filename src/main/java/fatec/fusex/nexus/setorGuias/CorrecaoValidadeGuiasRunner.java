package fatec.fusex.nexus.setorGuias;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import fatec.fusex.nexus.guia.ValidadeObrigatoriaGuia;

/**
 * T16-BE3: dispara a correcao das Guias antigas na inicializacao e, em seguida, torna validade_ate obrigatoria se
 * nenhuma Guia ficou sem validade (a migration V13 faz o mesmo antes; este passo cobre as Guias corrigidas a mao
 * depois). Liga por padrao; nexus.guia.correcao-validade.ativa=false desliga a correcao (depois que a equipe rodar
 * em producao). O NOT NULL so roda em MySQL: os testes usam H2, com o schema criado pelo Hibernate.
 */
@Component
@ConditionalOnProperty(name = "nexus.guia.correcao-validade.ativa", havingValue = "true", matchIfMissing = true)
public class CorrecaoValidadeGuiasRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CorrecaoValidadeGuiasRunner.class);

    private final CorrecaoValidadeGuias correcaoValidadeGuias;
    private final DataSource dataSource;

    public CorrecaoValidadeGuiasRunner(CorrecaoValidadeGuias correcaoValidadeGuias, DataSource dataSource) {
        this.correcaoValidadeGuias = correcaoValidadeGuias;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        correcaoValidadeGuias.executar(); // vazio quando a rotina ja rodou antes: nada a fazer
        try (Connection conexao = dataSource.getConnection()) {
            if ("MySQL".equalsIgnoreCase(conexao.getMetaData().getDatabaseProductName())
                    && ValidadeObrigatoriaGuia.aplicarSeNaoHouverNulos(conexao)) {
                LOG.info("validade_ate passou a ser obrigatoria: nenhuma Guia sem validade.");
            }
        }
    }
}
