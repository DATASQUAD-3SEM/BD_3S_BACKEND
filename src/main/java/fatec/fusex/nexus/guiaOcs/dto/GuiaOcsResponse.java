package fatec.fusex.nexus.guiaOcs.dto;

import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;

/**
 * Guia recebida pela OCS (T19-BE1): cabecalho, beneficiario, procedimentos, data de envio e situacao.
 * Nao traz CPF, sessoes nem dados da assinatura do beneficiario.
 */
public record GuiaOcsResponse(
        Long id,
        String numero,
        BeneficiarioDto beneficiario,
        List<ProcedimentoDto> procedimentos,
        LocalDateTime dataEnvio,
        SituacaoExecucao situacao) {

    public record BeneficiarioDto(String nome, String precCp) {}

    public static GuiaOcsResponse fromEntity(Guia guia, List<ProcedimentoDto> procedimentos) {
        var beneficiario = guia.getPreGuia().getBeneficiario();
        return new GuiaOcsResponse(
                guia.getIdGuia(),
                guia.getNumero(),
                new BeneficiarioDto(beneficiario.getNome(), beneficiario.getPrecCp()),
                procedimentos,
                guia.getDataEnvio(),
                guia.getSituacaoExecucao());
    }
}
