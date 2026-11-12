package fatec.fusex.nexus.guiaBeneficiario.dto;

import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guia.Guia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Guia do beneficiario (T18-BE1): cabecalho, situacao, procedimentos e sessoes.
 * {@code validadeAte} e nulo em Guia antiga sem validade gravada (ver T16).
 */
public record GuiaBeneficiarioResponse(
        Long id,
        String numero,
        String ocs,
        LocalDate validadeAte,
        LocalDateTime dataEnvio,
        SituacaoExecucao situacao,
        List<ProcedimentoDto> procedimentos,
        List<SessaoDto> sessoes) {

    public record ProcedimentoDto(String codDgp, String descricao, Integer quantidade) {

        public static ProcedimentoDto fromEntity(GuiaProcedimento procedimento) {
            return new ProcedimentoDto(
                    procedimento.getCodDgp(),
                    procedimento.getDescricaoProcedimento(),
                    procedimento.getQuantidadeAutorizada());
        }
    }

    /** {@code procedimento} traz o Cod DGP do exame da sessao. */
    public record SessaoDto(
            Long id,
            String procedimento,
            Integer numero,
            String status,
            LocalDateTime dataSolicitacao,
            LocalDateTime dataAssinatura) {

        public static SessaoDto fromEntity(Sessao sessao) {
            return new SessaoDto(
                    sessao.getIdSessao(),
                    sessao.getGuiaProcedimento().getCodDgp(),
                    sessao.getNumero(),
                    sessao.getStatus().name(),
                    sessao.getDataSolicitacao(),
                    sessao.getDataAssinatura());
        }
    }

    public static GuiaBeneficiarioResponse fromEntity(Guia guia,
                                                      List<ProcedimentoDto> procedimentos,
                                                      List<SessaoDto> sessoes) {
        return new GuiaBeneficiarioResponse(
                guia.getIdGuia(),
                guia.getNumero(),
                guia.getPreGuia().getOcs().getNome(),
                guia.getDataValidadeAte(),
                guia.getDataEnvio(),
                guia.getSituacaoExecucao(),
                procedimentos,
                sessoes);
    }
}
