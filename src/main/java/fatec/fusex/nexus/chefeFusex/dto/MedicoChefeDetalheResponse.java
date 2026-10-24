package fatec.fusex.nexus.chefeFusex.dto;

import fatec.fusex.nexus.medicoFusex.MedicoFusex;

/** Dados do medico no modo de edicao do chefe (T8-BE3). Nunca expoe a senha. */
public record MedicoChefeDetalheResponse(
        Long id,
        String nome,
        String patente,
        String cpf,
        String precCp,
        String crm,
        boolean ativo) {

    public static MedicoChefeDetalheResponse de(MedicoFusex m) {
        return new MedicoChefeDetalheResponse(
                m.getUsuarioId(),
                m.getNome(),
                m.getPatente(),
                m.getUsuario().getLogin(),
                m.getPrecCp(),
                m.getCrm(),
                Boolean.TRUE.equals(m.getUsuario().getAtivo()));
    }

    // Mantemos o toString sem CPF, por padrao do projeto (nao registrar CPF em log).
    @Override
    public String toString() {
        return "MedicoChefeDetalheResponse[id=" + id + "]";
    }
}
