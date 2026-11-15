package fatec.fusex.nexus.guiaFinalizada.dto;

import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.StatusEspelho;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;
import fatec.fusex.nexus.guiaFinalizada.VinculoEspelho;

public record GuiaFinalizadaResponse(
        Long id,
        String numero,
        BeneficiarioDto beneficiario,
        List<ProcedimentoDto> procedimentos,
        LocalDateTime dataEnvio,
        SituacaoExecucao situacao,
        VinculoEspelho vinculo,
        Long idEspelho) {

    public record BeneficiarioDto(String nome, String precCp) {}

    public static GuiaFinalizadaResponse fromEntity(Guia guia, List<ProcedimentoDto> procedimentos) {
        var beneficiario = guia.getPreGuia().getBeneficiario();
        Espelho espelho = guia.getEspelho();
        return new GuiaFinalizadaResponse(
                guia.getIdGuia(),
                guia.getNumero(),
                new BeneficiarioDto(beneficiario.getNome(), beneficiario.getPrecCp()),
                procedimentos,
                guia.getDataEnvio(),
                guia.getSituacaoExecucao(),
                vinculoDe(espelho),
                espelho == null ? null : espelho.getIdEspelho());
    }

    private static VinculoEspelho vinculoDe(Espelho espelho) {
        if (espelho == null) {
            return VinculoEspelho.DISPONIVEL;
        }
        return espelho.getStatus() == StatusEspelho.EM_CONSTRUCAO
                ? VinculoEspelho.EM_CONSTRUCAO
                : VinculoEspelho.ENVIADA;
    }
}