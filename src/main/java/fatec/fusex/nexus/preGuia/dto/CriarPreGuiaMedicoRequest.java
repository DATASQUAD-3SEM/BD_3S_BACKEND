package fatec.fusex.nexus.preGuia.dto;

import java.util.List;

/**
 * Corpo do POST /medicos/pre-guias (T2-BE4).
 * As validacoes ficam no MedicoPreGuiaService, porque a task pede 422 apontando o campo.
 */
public record CriarPreGuiaMedicoRequest(
        Long beneficiarioId,
        Long ocsId,
        List<String> procedimentos) {
}
