package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.TipoDependente;

/** Dependente na tela do chefe (T31-BE1). vigenciaVencida e so para exibicao: nada e encerrado por rotina. */
public record DependenteItemResponse(
        Long dependenteId,
        String nome,
        String vinculo,
        String precCp,
        String cpf,
        String telefone,
        LocalDate dataNascimento,
        int idade,
        TipoDependente tipo,
        LocalDate vigenciaFim,
        boolean vigenciaVencida,
        boolean temConta,
        List<ResponsavelDependenteResponse> responsaveis) {

    public static DependenteItemResponse de(Dependente d, LocalDate hoje, List<ResponsavelDependenteResponse> responsaveis) {
        return new DependenteItemResponse(
                d.getIdDependente(),
                d.getNome(),
                d.getVinculo(),
                d.getPrecCp(),
                d.getCpf(),
                d.getTelefone(),
                d.getDataNascimento(),
                Period.between(d.getDataNascimento(), hoje).getYears(),
                d.getTipo(),
                d.getVigenciaFim(),
                d.getVigenciaFim() != null && d.getVigenciaFim().isBefore(hoje),
                d.getUsuario() != null,
                responsaveis);
    }
}
