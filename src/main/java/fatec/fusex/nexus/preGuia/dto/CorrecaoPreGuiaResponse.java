package fatec.fusex.nexus.preGuia.dto;

import java.time.LocalDateTime;

public record CorrecaoPreGuiaResponse(
        String numero,
        String status,
        BeneficiarioResumo beneficiario,
        LocalDateTime dataCriacao,
        ObservacaoResumo observacao,
        EncaminhamentoResumo encaminhamentoAtual,
        OcsResumo ocsAtual) {

    public record BeneficiarioResumo(String nome, String precCp) {}

    public record ObservacaoResumo(String texto, LocalDateTime dataRegistro) {}

    // nome = so o nome do arquivo (o caminho nunca e exposto); tipo = Content-Type (ex: application/pdf)
    public record EncaminhamentoResumo(String nome, String tipo, LocalDateTime dataEnvio) {}

    public record OcsResumo(Long id, String nome) {}
}
