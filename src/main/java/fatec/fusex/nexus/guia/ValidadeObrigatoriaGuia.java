package fatec.fusex.nexus.guia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * T16-BE3: Guia.validade_ate passa a ser obrigatoria somente quando nenhuma Guia estiver sem validade. Usado pela
 * migration V13 (depois da correção das Guias antigas) e pelo runner de correção, para o caso em que a equipe
 * corrige as ultimas Guias a mao depois da migration.
 */
public final class ValidadeObrigatoriaGuia {

    private ValidadeObrigatoriaGuia() {
    }

    /** Quantas Guias estao sem validade gravada. */
    public static long semValidade(Connection conexao) throws SQLException {
        try (Statement sql = conexao.createStatement();
             ResultSet resultado = sql.executeQuery("SELECT COUNT(*) FROM Guia WHERE validade_ate IS NULL")) {
            resultado.next();
            return resultado.getLong(1);
        }
    }

    /**
     * Torna validade_ate NOT NULL se nao houver Guia sem validade e a coluna ainda aceitar nulo. Devolve true se
     * aplicou a alteracao. Em MySQL, a coluna e lida do information_schema para nao repetir o DDL a cada subida.
     */
    public static boolean aplicarSeNaoHouverNulos(Connection conexao) throws SQLException {
        boolean mysql = "MySQL".equalsIgnoreCase(conexao.getMetaData().getDatabaseProductName());
        if (mysql && !colunaAceitaNulo(conexao)) {
            return false;
        }
        if (semValidade(conexao) > 0) {
            return false;
        }
        String ddl = mysql
                ? "ALTER TABLE Guia MODIFY validade_ate DATE NOT NULL"
                : "ALTER TABLE Guia ALTER COLUMN validade_ate SET NOT NULL";
        try (Statement sql = conexao.createStatement()) {
            sql.execute(ddl);
        }
        return true;
    }

    private static boolean colunaAceitaNulo(Connection conexao) throws SQLException {
        String consulta = "SELECT IS_NULLABLE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Guia' AND COLUMN_NAME = 'validade_ate'";
        try (Statement sql = conexao.createStatement(); ResultSet resultado = sql.executeQuery(consulta)) {
            return resultado.next() && "YES".equalsIgnoreCase(resultado.getString(1));
        }
    }
}
