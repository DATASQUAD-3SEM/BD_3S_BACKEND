package fatec.fusex.nexus.setorGuias;

import java.text.Normalizer;
import java.util.Locale;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;


public enum StatusFila {

    PENDENTE("pendente", PreGuiaStatus.PENDENTE),
    EM_ANALISE("em analise", PreGuiaStatus.EM_ANALISE),
    ATENCAO("atencao", PreGuiaStatus.ATENCAO),
    CONCLUIDA("concluida", PreGuiaStatus.CONCLUIDA);

    private final String rotuloNormalizado;
    private final PreGuiaStatus status;

    StatusFila(String rotuloNormalizado, PreGuiaStatus status) {
        this.rotuloNormalizado = rotuloNormalizado;
        this.status = status;
    }

    public PreGuiaStatus getStatus() {
        return status;
    }

    public static PreGuiaStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalizado = normalizar(raw);
        for (StatusFila s : values()) {
            if (s.rotuloNormalizado.equals(normalizado)) {
                return s.status;
            }
        }
        throw new RegraDeNegocioException(
                "Status invalido. Use: Pendente, Em analise, Atencao ou Concluida");
    }

    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}