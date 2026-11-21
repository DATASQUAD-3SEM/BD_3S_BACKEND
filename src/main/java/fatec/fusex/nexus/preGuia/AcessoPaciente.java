package fatec.fusex.nexus.preGuia;

import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Regra unica de quem pode ver e agir na pre-guia e na Guia (T30-BE3, modelo 2.7):
 * sem dependente, o titular; MENOR_TITULAR, o titular; MENOR_RESPONSAVEL, o responsavel externo;
 * MAIOR, o proprio dependente (conta propria). O titular nao age pelo maior nem pelo menor sob responsavel.
 */
public final class AcessoPaciente {

    private AcessoPaciente() {
    }

    public static boolean podeAgir(Usuario usuario, PreGuia preGuia) {
        if (usuario == null || preGuia == null) {
            return false;
        }
        Dependente dependente = preGuia.getDependente();
        if (dependente == null) {
            return usuario.getIdUsuario().equals(preGuia.getBeneficiario().getUsuarioId());
        }
        if (dependente.getTipo() == TipoDependente.MAIOR) {
            return dependente.getUsuario() != null && usuario.getIdUsuario().equals(dependente.getUsuario().getIdUsuario());
        }
        if (dependente.getTipo() == TipoDependente.MENOR_RESPONSAVEL) {
            return dependente.getResponsavel() != null
                    && usuario.getIdUsuario().equals(dependente.getResponsavel().getIdUsuario());
        }
        return usuario.getIdUsuario().equals(preGuia.getBeneficiario().getUsuarioId());
    }
}
