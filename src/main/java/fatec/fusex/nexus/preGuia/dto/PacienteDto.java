package fatec.fusex.nexus.preGuia.dto;

import java.time.LocalDate;

import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Paciente de uma pre-guia ou Guia, para as listas do beneficiario e do responsavel legal (correcao de T30-BE-2/BE-3).
 * dependenteId nulo = o proprio titular. voceResponsavel = o usuario logado e o responsavel legal deste menor.
 * vigenciaFim so vem preenchida para dependente; e a data em que o acesso ao FUSEX foi encerrado.
 */
public record PacienteDto(
        Long dependenteId,
        String nome,
        String relacao,
        boolean menor,
        boolean voceResponsavel,
        LocalDate vigenciaFim) {

    public static PacienteDto de(PreGuia preGuia, Usuario logado) {
        Dependente dependente = preGuia.getDependente();
        if (dependente == null) {
            return new PacienteDto(null, preGuia.getBeneficiario().getNome(), "Titular", false, false, null);
        }
        boolean menor = dependente.getTipo() != TipoDependente.MAIOR;
        boolean responsavel = menor
                && dependente.getResponsavel() != null
                && dependente.getResponsavel().getIdUsuario().equals(logado.getIdUsuario());
        return new PacienteDto(
                dependente.getIdDependente(),
                dependente.getNome(),
                dependente.getVinculo(),
                menor,
                responsavel,
                dependente.getVigenciaFim());
    }
}
