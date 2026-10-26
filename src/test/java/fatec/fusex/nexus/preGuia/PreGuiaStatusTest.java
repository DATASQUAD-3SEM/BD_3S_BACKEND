package fatec.fusex.nexus.preGuia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Teste dos valores do status da pre-guia.
 */
class PreGuiaStatusTest {

    @Test
    void novaPreGuiaComecaComoPendente() {
        assertEquals(PreGuiaStatus.PENDENTE, new PreGuia().getStatus());
    }
}