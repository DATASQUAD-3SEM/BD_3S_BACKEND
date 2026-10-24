package fatec.fusex.nexus.MinhasPreGuias;

import java.util.Locale;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;

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