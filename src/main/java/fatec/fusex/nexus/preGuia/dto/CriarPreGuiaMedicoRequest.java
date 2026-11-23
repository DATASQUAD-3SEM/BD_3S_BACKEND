package fatec.fusex.nexus.preGuia.dto;

import java.util.List;

/**
 * Corpo do POST /medicos/pre-guias (T2-BE4, T30-BE7).
 * dependenteId (opcional): o paciente e um dependente vigente; o titular e o do dependente.
 * As validacoes ficam no MedicoPreGuiaService, porque a task pede 422 apontando o campo.
 */
public record CriarPreGuiaMedicoRequest(
        Long beneficiarioId,
        Long ocsId,
        List<String> procedimentos,
        Long dependenteId) {

    public CriarPreGuiaMedicoRequest(Long beneficiarioId, Long ocsId, List<String> procedimentos) {
        this(beneficiarioId, ocsId, procedimentos, null);
    }
}
