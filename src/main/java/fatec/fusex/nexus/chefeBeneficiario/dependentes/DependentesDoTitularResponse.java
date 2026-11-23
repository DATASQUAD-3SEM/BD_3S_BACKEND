package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import java.util.List;

/** Lista de dependentes de um titular: vigentes primeiro, depois os de vigencia vencida (T31-BE1). */
public record DependentesDoTitularResponse(Long titularId, String titularNome, List<DependenteItemResponse> dependentes) {
}
