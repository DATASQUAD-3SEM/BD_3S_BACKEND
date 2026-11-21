package fatec.fusex.nexus.setorFatura;

import static fatec.fusex.nexus.setorFatura.ControleFluxoFatura.ARQUIVO;
import static fatec.fusex.nexus.setorFatura.ControleFluxoFatura.AUDITAGEM;
import static fatec.fusex.nexus.setorFatura.ControleFluxoFatura.GLOSA_OCS;
import static fatec.fusex.nexus.setorFatura.ControleFluxoFatura.LISURA_GLOSA;
import static fatec.fusex.nexus.setorFatura.ControleFluxoFatura.PROTOCOLO;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.espelho.EspelhoItem;

class ControleFluxoFaturaTest {

    private static final LocalDateTime ENVIO = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final BigDecimal INFORMADO = new BigDecimal("300.00");
    private static final BigDecimal FINAL_LISURA = new BigDecimal("280.00");

    @Test
    void semProtocoloNaoHaLinhas() {
        assertThat(ControleFluxoFatura.calcular(ENVIO, List.of(), INFORMADO, null)).isEmpty();
    }

    @Test
    void faturaSemGlosaPulaDeLisuraDiretoParaAuditagem() {
        List<FaturaEvento> eventos = List.of(
                evento(TipoFaturaEvento.PROTOCOLADA, dia(11), "Ana Silva"),
                evento(TipoFaturaEvento.ENVIADA_LISURA, dia(12), "Bruno Lima"),
                evento(TipoFaturaEvento.ENVIADA_AUDITORIA, dia(13), "Carla Dias"),
                evento(TipoFaturaEvento.VISUALIZADA, dia(14), "Dani Reis"));

        List<EtapaFluxo> etapas = ControleFluxoFatura.calcular(ENVIO, eventos, INFORMADO, FINAL_LISURA);

        assertThat(etapas).extracting(EtapaFluxo::etapa)
                .containsExactly(PROTOCOLO, LISURA_GLOSA, AUDITAGEM, ARQUIVO);
        assertThat(etapas.get(0).responsavel()).isEqualTo("Bruno Lima");
        assertThat(etapas.get(0).valorInicial()).isEqualByComparingTo(INFORMADO);
        assertThat(etapas.get(1).dataSaida()).isEqualTo(dia(13));
        assertThat(etapas.get(1).responsavel()).isEqualTo("Carla Dias");
        assertThat(etapas.get(2).responsavel()).isEqualTo("Dani Reis");
        assertThat(etapas.get(3).dataSaida()).isNull();
    }

    @Test
    void faturaComGlosaTemLinhaGlosaOcsEntreLisuraEAuditagem() {
        List<FaturaEvento> eventos = List.of(
                evento(TipoFaturaEvento.PROTOCOLADA, dia(11), "Ana Silva"),
                evento(TipoFaturaEvento.ENVIADA_LISURA, dia(12), "Bruno Lima"),
                evento(TipoFaturaEvento.ENVIADA_OCS, dia(13), "Carla Dias"),
                evento(TipoFaturaEvento.CIENTE_OCS, dia(14), "Contato OCS"),
                evento(TipoFaturaEvento.ENVIADA_AUDITORIA, dia(15), "Carla Dias"),
                evento(TipoFaturaEvento.VISUALIZADA, dia(16), "Dani Reis"));

        List<EtapaFluxo> etapas = ControleFluxoFatura.calcular(ENVIO, eventos, INFORMADO, FINAL_LISURA);

        assertThat(etapas).extracting(EtapaFluxo::etapa)
                .containsExactly(PROTOCOLO, LISURA_GLOSA, GLOSA_OCS, AUDITAGEM, ARQUIVO);
        assertThat(etapas.get(1).dataSaida()).isEqualTo(dia(13));
        assertThat(etapas.get(2).dataEntrada()).isEqualTo(dia(13));
        assertThat(etapas.get(2).dataSaida()).isEqualTo(dia(14));
        assertThat(etapas.get(2).responsavel()).isEqualTo("Contato OCS");
    }

    @Test
    void contestacaoRepeteParUmaRodadaAteNaoHaverMaisGlosa() {
        List<FaturaEvento> eventos = List.of(
                evento(TipoFaturaEvento.PROTOCOLADA, dia(11), "Ana Silva"),
                evento(TipoFaturaEvento.ENVIADA_LISURA, dia(12), "Bruno Lima"),
                evento(TipoFaturaEvento.ENVIADA_OCS, dia(13), "Carla Dias"),
                evento(TipoFaturaEvento.CONTESTADA_OCS, dia(14), "Contato OCS"),
                evento(TipoFaturaEvento.ENVIADA_OCS, dia(15), "Carla Dias"),
                evento(TipoFaturaEvento.CIENTE_OCS, dia(16), "Contato OCS"),
                evento(TipoFaturaEvento.ENVIADA_AUDITORIA, dia(17), "Carla Dias"),
                evento(TipoFaturaEvento.VISUALIZADA, dia(18), "Dani Reis"));

        List<EtapaFluxo> etapas = ControleFluxoFatura.calcular(ENVIO, eventos, INFORMADO, FINAL_LISURA);

        assertThat(etapas).extracting(EtapaFluxo::etapa).containsExactly(
                PROTOCOLO, LISURA_GLOSA, GLOSA_OCS, LISURA_GLOSA, GLOSA_OCS, AUDITAGEM, ARQUIVO);
        assertThat(etapas.get(3).dataEntrada()).isEqualTo(dia(14));
        assertThat(etapas.get(3).dataSaida()).isEqualTo(dia(15));
        assertThat(etapas.get(4).dataEntrada()).isEqualTo(dia(15));
        assertThat(etapas.get(4).dataSaida()).isEqualTo(dia(16));
    }

    @Test
    void faturaEmAndamentoDeixaUltimaLinhaSemSaida() {
        List<FaturaEvento> eventos = List.of(
                evento(TipoFaturaEvento.PROTOCOLADA, dia(11), "Ana Silva"),
                evento(TipoFaturaEvento.ENVIADA_LISURA, dia(12), "Bruno Lima"),
                evento(TipoFaturaEvento.ENVIADA_OCS, dia(13), "Carla Dias"));

        List<EtapaFluxo> etapas = ControleFluxoFatura.calcular(ENVIO, eventos, INFORMADO, null);

        assertThat(etapas).extracting(EtapaFluxo::etapa)
                .containsExactly(PROTOCOLO, LISURA_GLOSA, GLOSA_OCS);
        assertThat(etapas.get(2).dataSaida()).isNull();
        assertThat(etapas.get(2).responsavel()).isNull();
    }

    @Test
    void auditagemEmAndamentoNaoTemLinhaArquivo() {
        List<FaturaEvento> eventos = List.of(
                evento(TipoFaturaEvento.PROTOCOLADA, dia(11), "Ana Silva"),
                evento(TipoFaturaEvento.ENVIADA_LISURA, dia(12), "Bruno Lima"),
                evento(TipoFaturaEvento.ENVIADA_AUDITORIA, dia(13), "Carla Dias"));

        List<EtapaFluxo> etapas = ControleFluxoFatura.calcular(ENVIO, eventos, INFORMADO, null);

        assertThat(etapas).extracting(EtapaFluxo::etapa)
                .containsExactly(PROTOCOLO, LISURA_GLOSA, AUDITAGEM);
        assertThat(etapas.get(2).dataSaida()).isNull();
    }

    @Test
    void valorInformadoSomaSessoesVezesQuantidade() {
        EspelhoItem a = item("100.00", 2);
        EspelhoItem b = item("50.50", 1);

        assertThat(ControleFluxoFatura.valorInformado(List.of(a, b))).isEqualByComparingTo("250.50");
    }

    private static EspelhoItem item(String valorSessao, int quantidade) {
        EspelhoItem item = new EspelhoItem();
        item.setValorSessao(new BigDecimal(valorSessao));
        item.setQuantidadeSessoes(quantidade);
        return item;
    }

    private static FaturaEvento evento(TipoFaturaEvento tipo, LocalDateTime data, String responsavel) {
        FaturaEvento evento = new FaturaEvento();
        evento.setTipo(tipo);
        evento.setDataEvento(data);
        evento.setResponsavel(responsavel);
        return evento;
    }

    private static LocalDateTime dia(int dia) {
        return LocalDateTime.of(2026, 11, dia, 10, 0);
    }
}