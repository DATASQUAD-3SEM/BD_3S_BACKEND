package fatec.fusex.nexus.correcaoMedico.dto;

import java.time.LocalDateTime;

import fatec.fusex.nexus.preGuia.PreGuia;

/** Cartao da aba "Corrigir pre-guias" do medico (T14-BE1). */
public record PreGuiaParaCorrigirResponse(
        Long id,
        String numero,
        String beneficiarioNome,
        String precCp,
        String ocsNome,
        LocalDateTime dataCriacao,
        String observacao) {

    public static PreGuiaParaCorrigirResponse de(PreGuia preGuia, String observacao) {
        return new PreGuiaParaCorrigirResponse(
                preGuia.getIdPreGuia(),
                String.format("%04d", preGuia.getIdPreGuia()), // mesmo numero do MinhaPreGuiaResponse
                preGuia.getBeneficiario().getNome(),
                preGuia.getBeneficiario().getPrecCp(),
                preGuia.getOcs().getNome(),
                preGuia.getDataCriacao(),
                observacao);
    }
}
