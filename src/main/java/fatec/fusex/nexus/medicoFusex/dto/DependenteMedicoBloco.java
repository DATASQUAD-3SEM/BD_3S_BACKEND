package fatec.fusex.nexus.medicoFusex.dto;

import java.util.List;

/** Bloco do dependente na busca do medico (T30-BE7). */
public record DependenteMedicoBloco(
        Long dependenteId,
        String titularNome,
        String titularPrecCp,
        String vinculo,
        boolean menor,
        List<String> responsaveis) {
}
