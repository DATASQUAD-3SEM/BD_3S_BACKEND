package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.dependente.Dependente;

class PacienteDaPreGuiaTest {

    @Test
    void pacienteTitularUsaOPrecCpDoTitular() {
        PreGuia preGuia = preGuia(null);

        assertThat(PacienteDaPreGuia.precCp(preGuia)).isEqualTo("45872213300");
        assertThat(PacienteDaPreGuia.nome(preGuia)).isEqualTo("Antonio Titular");
    }

    @Test
    void pacienteDependenteUsaOPrecCpDoDependente() {
        Dependente dependente = new Dependente();
        dependente.setNome("Miguel Ferreira");
        dependente.setPrecCp("123.456.789-02");
        PreGuia preGuia = preGuia(dependente);

        assertThat(PacienteDaPreGuia.precCp(preGuia)).isEqualTo("12345678902");
        assertThat(PacienteDaPreGuia.nome(preGuia)).isEqualTo("Miguel Ferreira");
    }

    private static PreGuia preGuia(Dependente dependente) {
        Beneficiario titular = new Beneficiario();
        titular.setNome("Antonio Titular");
        titular.setPrecCp("45872213300");
        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(titular);
        preGuia.setDependente(dependente);
        return preGuia;
    }
}
