package fatec.fusex.nexus.medicoFusex.dto;

import fatec.fusex.nexus.medicoFusex.MedicoFusex;


/**
 * Resposta do GET /medicos/me (T2-BE2, T34-BE1): nome, patente, CRM, CPF e Prec CP do medico logado, para o bloco
 * "Medico solicitante" da Nova pre-guia (regra 2, rota 2). O CPF e o login da conta (so digitos) e o Prec CP vem de
 * MedicoFusex.prec_cp (so digitos). O toString omite os dados do medico, para nao aparecerem em log.
 */
public record MedicoMeResponse(
        String nome,
        String patente,
        String crm,
        String cpf,
        String precCp) {

    /** login: o login da conta do medico logado, que e o CPF dele. */
    public static MedicoMeResponse de(MedicoFusex m, String login) {
        return new MedicoMeResponse(
                m.getNome(),
                m.getPatente(),
                m.getCrm(),
                login,
                m.getPrecCp());
    }

    // Mantemos o toString sem dados do medico, por padrao do projeto (o CPF nunca vai para log).
    @Override
    public String toString() {
        return "MedicoMeResponse[crm=***]";
    }
}
