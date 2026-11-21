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
        String pacienteNome,
        // T30-BE4: Prec CP esperado (o do dependente quando a pre-guia tem dependente) e se o paciente e dependente.
        String precCpPaciente,
        Boolean pacienteDependente) {
}
