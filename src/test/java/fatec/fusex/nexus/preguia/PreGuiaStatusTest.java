package fatec.fusex.nexus.preguia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreGuiaStatusTest {

    @Test
    void novaPreGuiaComecaComoRascunho() {
        PreGuia preGuia = new PreGuia();

        assertEquals(
                StatusPreGuia.RASCUNHO,
                preGuia.getStatus()
        );
    }

    @Test
    void confirmarEnvioMudaStatusParaPendente() {
        PreGuia preGuia = new PreGuia();

        preGuia.confirmarEnvio();

        assertEquals(
                StatusPreGuia.PENDENTE,
                preGuia.getStatus()
        );
    }

    @Test
    void iniciarAnaliseMudaStatusParaEmAnalise() {
        PreGuia preGuia = new PreGuia();

        preGuia.confirmarEnvio();
        preGuia.iniciarAnalise();

        assertEquals(
                StatusPreGuia.EM_ANALISE,
                preGuia.getStatus()
        );
    }

    @Test
    void aprovarMudaStatusParaAprovada() {
        PreGuia preGuia = new PreGuia();

        preGuia.confirmarEnvio();
        preGuia.iniciarAnalise();
        preGuia.aprovar();

        assertEquals(
                StatusPreGuia.APROVADA,
                preGuia.getStatus()
        );
    }

    @Test
    void naoPermiteAprovarAntesDaAnalise() {
        PreGuia preGuia = new PreGuia();

        assertThrows(
                IllegalStateException.class,
                preGuia::aprovar
        );
    }

    @Test
    void naoPermiteIniciarAnaliseAntesDoEnvio() {
        PreGuia preGuia = new PreGuia();

        assertThrows(
                IllegalStateException.class,
                preGuia::iniciarAnalise
        );
    }

    @Test
    void naoPermiteEnviarPreGuiaNovamente() {
        PreGuia preGuia = new PreGuia();

        preGuia.confirmarEnvio();

        assertThrows(
                IllegalStateException.class,
                preGuia::confirmarEnvio
        );
    }
}