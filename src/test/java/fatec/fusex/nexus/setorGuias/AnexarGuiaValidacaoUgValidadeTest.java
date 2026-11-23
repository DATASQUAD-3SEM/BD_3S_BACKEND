package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;

// T16-BE2: regras de UG, UG Origem e validade do PUT guia-rascunho (sem contexto Spring)
class AnexarGuiaValidacaoUgValidadeTest {

    private static final String MSG_VAZIA = "Informe a UG impressa na Guia";
    private static final String MSG_NAO_FUSEX = "A Guia não é do FUSEX. A UG deve ser Cmdo Bda Inf Amv.";
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 18);

    @Test
    void ugDoFusexEmQualquerCaixaEEspacamento_eAceita() {
        assertThatCode(() -> AnexarGuiaService.validarUg("Cmdo Bda Inf Amv", "ug", MSG_VAZIA, MSG_NAO_FUSEX))
                .doesNotThrowAnyException();
        assertThatCode(() -> AnexarGuiaService.validarUg("  CMDO   bda  inf   AMV ", "ug", MSG_VAZIA, MSG_NAO_FUSEX))
                .doesNotThrowAnyException();
    }

    @Test
    void ugDiferente_devolveErroNoCampo() {
        assertThatThrownBy(() -> AnexarGuiaService.validarUg("Cmdo Bda Inf Sul", "ug", MSG_VAZIA, MSG_NAO_FUSEX))
                .isInstanceOfSatisfying(DadosInvalidosException.class, e -> {
                    assertThat(e.getCampo()).isEqualTo("ug");
                    assertThat(e.getMessage()).isEqualTo(MSG_NAO_FUSEX);
                });
    }

    @Test
    void ugVaziaOuSoEspacos_devolveMensagemDeVazio() {
        assertThatThrownBy(() -> AnexarGuiaService.validarUg("   ", "ug", MSG_VAZIA, MSG_NAO_FUSEX))
                .isInstanceOfSatisfying(DadosInvalidosException.class,
                        e -> assertThat(e.getMessage()).isEqualTo(MSG_VAZIA));
        assertThatThrownBy(() -> AnexarGuiaService.validarUg(null, "ug", MSG_VAZIA, MSG_NAO_FUSEX))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void ugComMaisDe100Caracteres_devolveErroDeTamanho() {
        String longa = "C".repeat(101);
        assertThatThrownBy(() -> AnexarGuiaService.validarUg(longa, "ug", MSG_VAZIA, MSG_NAO_FUSEX))
                .isInstanceOfSatisfying(DadosInvalidosException.class,
                        e -> assertThat(e.getMessage()).isEqualTo("A UG deve ter no maximo 100 caracteres"));
    }

    @Test
    void validadeFutura_eHoje_saoAceitas() {
        assertThat(AnexarGuiaService.validarValidade("2027-01-01", HOJE)).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(AnexarGuiaService.validarValidade("2026-11-18", HOJE)).isEqualTo(HOJE);
    }

    @Test
    void validadeAnteriorAHoje_devolveErroComDataBrasileira() {
        assertThatThrownBy(() -> AnexarGuiaService.validarValidade("2026-11-17", HOJE))
                .isInstanceOfSatisfying(DadosInvalidosException.class, e -> {
                    assertThat(e.getCampo()).isEqualTo("validadeAte");
                    assertThat(e.getMessage()).isEqualTo("Esta Guia já venceu em 17/11/2026. Não é possível concluir.");
                });
    }

    @Test
    void validadeAusenteOuForaDoFormato_devolveErro() {
        assertThatThrownBy(() -> AnexarGuiaService.validarValidade(" ", HOJE))
                .isInstanceOfSatisfying(DadosInvalidosException.class,
                        e -> assertThat(e.getMessage()).isEqualTo("Informe a validade da Guia"));
        assertThatThrownBy(() -> AnexarGuiaService.validarValidade("18/11/2026", HOJE))
                .isInstanceOfSatisfying(DadosInvalidosException.class,
                        e -> assertThat(e.getMessage()).isEqualTo("A validade deve estar no formato aaaa-mm-dd"));
        assertThatThrownBy(() -> AnexarGuiaService.validarValidade("2027-02-31", HOJE))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
