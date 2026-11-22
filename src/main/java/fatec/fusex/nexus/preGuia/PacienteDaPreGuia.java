package fatec.fusex.nexus.preGuia;

/**
 * Paciente de uma pre-guia (T30-BE4): o dependente quando existe, senao o titular.
 * Usado para conferir o Prec CP da Guia e para mostrar o nome do paciente.
 */
public final class PacienteDaPreGuia {

    private PacienteDaPreGuia() {
    }

    public static String nome(PreGuia preGuia) {
        if (preGuia.getDependente() != null) {
            return preGuia.getDependente().getNome();
        }
        return preGuia.getBeneficiario() == null ? null : preGuia.getBeneficiario().getNome();
    }

    /** Prec CP do paciente, so digitos; null quando nao cadastrado. */
    public static String precCp(PreGuia preGuia) {
        String precCp = preGuia.getDependente() != null
                ? preGuia.getDependente().getPrecCp()
                : (preGuia.getBeneficiario() == null ? null : preGuia.getBeneficiario().getPrecCp());
        return precCp == null || precCp.isBlank() ? null : precCp.replaceAll("[^0-9]", "");
    }
}
