package fatec.fusex.nexus.minhasPreGuias.dto;

import fatec.fusex.nexus.preGuia.PreGuiaStatus;

import java.time.LocalDateTime;

/**
 * Resposta do GET /beneficiarios/me/pre-guias/{id}/correcao (T9-BE1), com a observacao do setor,
 * o encaminhamento enviado e a OCS atual.
 */
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