package fatec.fusex.nexus.chefeFusex.dto;

import fatec.fusex.nexus.medicoFusex.MedicoFusex;

/** Linha da tabela "Medicos" do chefe (T8-BE1). cpf e precCp vao sem mascara. */
public record MedicoChefeItemResponse(
        Long id,
        String nome,
        String cpf,
        String precCp,
        String crm,
        boolean ativo) {

    public static MedicoChefeItemResponse de(MedicoFusex m) {
        return new MedicoChefeItemResponse(
                m.getUsuarioId(),
                m.getNome(),
                m.getUsuario().getLogin(),
                m.getPrecCp(),
                m.getCrm(),
                Boolean.TRUE.equals(m.getUsuario().getAtivo()));
    }

    // Mantemos o toString sem CPF, por padrao do projeto (nao registrar CPF em log).
    @Override
    public String toString() {
        return "MedicoChefeItemResponse[id=" + id + "]";
    }
}
