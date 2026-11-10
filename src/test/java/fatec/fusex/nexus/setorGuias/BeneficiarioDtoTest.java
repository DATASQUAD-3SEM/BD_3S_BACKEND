package fatec.fusex.nexus.setorGuias;

import fatec.fusex.nexus.setorGuias.dto.DetalhePreGuiaResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BeneficiarioDtoTest {

    @Test
    void toStringNaoDeveConterCpf() {
        DetalhePreGuiaResponse.BeneficiarioDto dto = new DetalhePreGuiaResponse.BeneficiarioDto(
                "Marta Souza Lima", "52998224725", "45810020001", "45-54"
        );

        String resultado = dto.toString();

        assertFalse(resultado.contains("52998224725"), "O CPF nao pode aparecer no toString");
    }
}