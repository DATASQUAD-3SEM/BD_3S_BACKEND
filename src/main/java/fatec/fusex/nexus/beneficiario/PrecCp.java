package fatec.fusex.nexus.beneficiario;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;


final class PrecCp {

    private static final int TAMANHO = 11;

    private PrecCp() {
    }


    static String normalizar(String informado) {
        if (informado == null || informado.isBlank()) {
            throw new RegraDeNegocioException("Informe o Prec CP");
        }

        String semMascara = informado.trim().replace(".", "").replace("-", "");

        if (!ehSoDigitos(semMascara) || semMascara.length() != TAMANHO) {
            throw new RegraDeNegocioException(
                    "Prec CP invalido: informe " + TAMANHO + " digitos, com ou sem pontos e hifen");
        }
        return semMascara;
    }

    // Nao usamos Character.isDigit: ele aceita digitos de outros alfabetos (ex: arabe).
    private static boolean ehSoDigitos(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
