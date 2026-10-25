package fatec.fusex.nexus.minhasPreGuias.dto;

import fatec.fusex.nexus.preGuia.PreGuiaStatus;

import java.time.LocalDateTime;

public record CorrecaoPreGuiaResponse(
        String numero,
        PreGuiaStatus status,
        BeneficiarioDto beneficiario,
        LocalDateTime dataCriacao,
        ObservacaoDto observacao,
        EncaminhamentoDto encaminhamentoAtual,
        OcsDto ocsAtual
) {
    public record BeneficiarioDto(String nome, String precCp) {}

    public record ObservacaoDto(String texto, LocalDateTime dataRegistro) {}

    public record EncaminhamentoDto(String nome, String tipo, LocalDateTime dataEnvio) {}

    public record OcsDto(Long id, String nome) {}
}