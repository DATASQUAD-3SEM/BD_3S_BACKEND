package fatec.fusex.nexus.setorGuias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.setorGuias.dto.ExameLidoResponse;

class LeitorGuiaPdfTest {

    @Test
    void precCpDoPaciente_pegaOValorDepoisDaMarcaPaciente() {
        String texto = "Titular: (PACIENTE)\nREGINA VERA MARQUES\nUG Origem: Cmdo\nPrec CP:\n98317046100\nAssinatura";

        assertEquals(Optional.of("98317046100"), LeitorGuiaPdf.precCpDoPaciente(texto));
    }

    @Test
    void precCpDoPaciente_ignoraOPrecCpDoTitularQuandoOPacienteEDependente() {
        String texto = "Titular: JOAO\nPrec CP: 11111111111\nDependente: (PACIENTE)\nMARIA\nPrec CP: 22222222222";

        assertEquals(Optional.of("22222222222"), LeitorGuiaPdf.precCpDoPaciente(texto));
    }

    @Test
    void precCpDoPaciente_semMarcaSemLinhaOuVazio_devolveVazio() {
        assertFalse(LeitorGuiaPdf.precCpDoPaciente("Titular: JOAO\nPrec CP: 11111111111").isPresent());
        assertFalse(LeitorGuiaPdf.precCpDoPaciente("(PACIENTE)\nMARIA").isPresent());
        assertFalse(LeitorGuiaPdf.precCpDoPaciente("").isPresent());
        assertFalse(LeitorGuiaPdf.precCpDoPaciente(null).isPresent());
    }

    @Test
    void precCpDoPaciente_guardaSoOsDigitosMesmoComPontosETracos() {
        assertEquals(Optional.of("98317046100"),
                LeitorGuiaPdf.precCpDoPaciente("(PACIENTE) Prec CP: 983.170.461-00 Assinatura"));
    }

    @Test
    void examesDaGuia_leCodigoDescricaoEQuantidadeDeCadaLinha() {
        String texto = "Procedimentos: Cod DGP: Quant V.Unit Pos-Aud:\n"
                + "Consulta ao oncologista 10101160 1 93,06\n"
                + "Exame sem quantidade 20011015 93,06\n"
                + "Cotista: DAP";

        assertEquals(
                List.of(
                        new ExameLidoResponse("10101160", "Consulta ao oncologista", 1),
                        new ExameLidoResponse("20011015", "Exame sem quantidade", null)),
                LeitorGuiaPdf.examesDaGuia(texto));
    }

    @Test
    void examesDaGuia_semQuadroDeProcedimentos_devolveListaVazia() {
        assertTrue(LeitorGuiaPdf.examesDaGuia("GUIA DE ENCAMINHAMENTO Nr: 202610678").isEmpty());
    }

    @Test
    void precCpConfere_ignoraFormatacaoEComparaSoDigitos() {
        assertTrue(LeitorGuiaPdf.precCpConfere("983.170.461-00", "98317046100"));
        assertFalse(LeitorGuiaPdf.precCpConfere("11111111111", "22222222222"));
    }
}