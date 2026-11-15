package fatec.fusex.nexus.guiaOcs.dto;

import java.time.LocalDate;
import java.util.List;

import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.ProcedimentoDto;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse.SessaoDto;
import fatec.fusex.nexus.guiaOcs.dto.GuiaOcsResponse.BeneficiarioDto;

/**
 * Modelo digitalizado da guia para a OCS (T19-BE2): numero, validade, beneficiario, procedimentos e sessoes.
 * {@code validadeAte} vem do PDF (NAO RECEBER APOS). Nao traz CPF nem dados da assinatura do beneficiario.
 */
public record GuiaOcsDetalheResponse(
        Long id,
        String numero,
        LocalDate validadeAte,
        BeneficiarioDto beneficiario,
        List<ProcedimentoDto> procedimentos,
        List<SessaoDto> sessoes) {

    public static GuiaOcsDetalheResponse fromEntity(Guia guia,
                                                    List<ProcedimentoDto> procedimentos,
                                                    List<SessaoDto> sessoes) {
        var beneficiario = guia.getPreGuia().getBeneficiario();
        return new GuiaOcsDetalheResponse(
                guia.getIdGuia(),
                guia.getNumero(),
                guia.getDataValidadeAte(),
                new BeneficiarioDto(beneficiario.getNome(), beneficiario.getPrecCp()),
                procedimentos,
                sessoes);
    }
}
