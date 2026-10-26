package fatec.fusex.nexus.correcaoMedico.dto;

import java.util.List;

/**
 * Corpo do PUT /medicos/me/pre-guias/{id}/correcao (T14-BE5).
 * ocsId e opcional (nulo mantem a OCS atual). O beneficiario nao pode ser trocado, entao nao entra aqui.
 */
public record SalvarCorrecaoRequest(
        Long ocsId,
        List<String> procedimentos) {
}
