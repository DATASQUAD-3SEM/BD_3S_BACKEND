package fatec.fusex.nexus.setorGuias.dto;

import java.util.List;

public record ExtracaoNumeroGuiaResponse(
        String numeroGuia,
        String precCpLido,
        Boolean precCpConfere,
        List<ExameLidoResponse> examesNaGuia,

        String ug,
        String ugOrigem,
        String validadeAte,
        String pacienteNome) {
}
