package fatec.fusex.nexus.minhasPreGuias.dto;

import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import java.time.LocalDateTime;
import java.time.LocalDate;

public record MinhaPreGuiaResponse(
        Long id,
        String numero,
        String ocsNome,
        String origem,
        CriadoPorMedicoDto criadoPorMedico,
        LocalDateTime dataCriacao,
        PreGuiaStatus status,
        String observacao,
        GuiaDto guia
) {
    public record CriadoPorMedicoDto(String nome, String patente) {}
    public record GuiaDto(String numero, LocalDate validadeAte) {}

    public static MinhaPreGuiaResponse fromEntity(PreGuia preGuia, String observacao, GuiaDto guiaDto) {

        // Deduz a origem: se tem médico, é MEDICO. Se não, é BENEFICIARIO.
        String origem = preGuia.getMedicoFusex() != null ? "MEDICO" : "BENEFICIARIO";

        CriadoPorMedicoDto medicoDto = null;
        if ("MEDICO".equals(origem) && preGuia.getMedicoFusex() != null) {
            medicoDto = new CriadoPorMedicoDto(
                    preGuia.getMedicoFusex().getNome(),
                    preGuia.getMedicoFusex().getPatente()
            );
        }

        return new MinhaPreGuiaResponse(
                preGuia.getIdPreGuia(),
                String.format("%04d", preGuia.getIdPreGuia()), // Formata com 4 dígitos
                preGuia.getOcs().getNome(),
                origem,
                medicoDto,
                preGuia.getDataCriacao(),
                preGuia.getStatus(),
                observacao,
                guiaDto
        );
    }
}