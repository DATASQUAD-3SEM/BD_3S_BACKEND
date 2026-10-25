package fatec.fusex.nexus.minhasPreGuias;

import java.util.Locale;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;

/**
 * Modo de entrega do PDF ao beneficiario: BAIXAR (attachment) ou VISUALIZAR (inline).
 */
public enum ModoDownload {
    BAIXAR, VISUALIZAR;

    public static ModoDownload from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DadosInvalidosException("modo", "Informe o modo: baixar ou visualizar");
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException("modo", "Modo invalido: use baixar ou visualizar");
        }
    }
}