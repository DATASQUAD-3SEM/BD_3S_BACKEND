package fatec.fusex.nexus.setorFatura;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import fatec.fusex.nexus.espelho.EspelhoItem;

public final class ControleFluxoFatura {

    public static final String PROTOCOLO = "Protocolo";
    public static final String LISURA_GLOSA = "Lisura / Glosa";
    public static final String GLOSA_OCS = "Glosa / OCS";
    public static final String AUDITAGEM = "Auditagem";
    public static final String ARQUIVO = "Arquivo";

    public static final String SETOR_FATURAS = "Setor de Faturas";
    public static final String SETOR_RASTREABILIDADE = "Rastreabilidade";
    public static final String SETOR_OCS = "OCS";
    public static final String SETOR_AUDITORIA = "Auditoria";

    private ControleFluxoFatura() {
    }

    public static BigDecimal valorInformado(List<EspelhoItem> itens) {
        return itens.stream()
                .map(item -> item.getValorSessao().multiply(BigDecimal.valueOf(item.getQuantidadeSessoes())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static List<EtapaFluxo> calcular(LocalDateTime dataEnvio, List<FaturaEvento> eventos,
                                            BigDecimal valorInformado, BigDecimal valorFinalLisura) {
        List<EtapaFluxo> etapas = new ArrayList<>();

        int protocolada = indiceDe(eventos, 0, TipoFaturaEvento.PROTOCOLADA);
        if (protocolada < 0) {
            return etapas;
        }

        int lisura = indiceDe(eventos, protocolada + 1, TipoFaturaEvento.ENVIADA_LISURA);
        FaturaEvento responsavelProtocolo = lisura < 0 ? eventos.get(protocolada) : eventos.get(lisura);
        etapas.add(new EtapaFluxo(PROTOCOLO, SETOR_FATURAS, dataEnvio,
                lisura < 0 ? null : eventos.get(lisura).getDataEvento(),
                responsavelProtocolo.getResponsavel(), valorInformado, null));
        if (lisura < 0) {
            return etapas;
        }

        LocalDateTime entradaLisura = eventos.get(lisura).getDataEvento();
        int posicao = lisura;
        while (true) {
            int proximoEnvio = indiceDe(eventos, posicao + 1,
                    TipoFaturaEvento.ENVIADA_OCS, TipoFaturaEvento.ENVIADA_AUDITORIA);
            if (proximoEnvio < 0) {
                etapas.add(new EtapaFluxo(LISURA_GLOSA, SETOR_RASTREABILIDADE, entradaLisura, null, null,
                        valorInformado, null));
                return etapas;
            }

            FaturaEvento envio = eventos.get(proximoEnvio);
            etapas.add(new EtapaFluxo(LISURA_GLOSA, SETOR_RASTREABILIDADE, entradaLisura,
                    envio.getDataEvento(), envio.getResponsavel(), valorInformado, valorFinalLisura));
            if (envio.getTipo() == TipoFaturaEvento.ENVIADA_AUDITORIA) {
                adicionarAuditagem(etapas, eventos, proximoEnvio, valorFinalLisura);
                return etapas;
            }

            int retorno = indiceDe(eventos, proximoEnvio + 1,
                    TipoFaturaEvento.CIENTE_OCS, TipoFaturaEvento.CONTESTADA_OCS);
            if (retorno < 0) {
                etapas.add(new EtapaFluxo(GLOSA_OCS, SETOR_OCS, envio.getDataEvento(), null, null, null, null));
                return etapas;
            }

            FaturaEvento resposta = eventos.get(retorno);
            etapas.add(new EtapaFluxo(GLOSA_OCS, SETOR_OCS, envio.getDataEvento(),
                    resposta.getDataEvento(), resposta.getResponsavel(), null, null));
            if (resposta.getTipo() == TipoFaturaEvento.CIENTE_OCS) {
                int auditoria = indiceDe(eventos, retorno + 1, TipoFaturaEvento.ENVIADA_AUDITORIA);
                if (auditoria >= 0) {
                    adicionarAuditagem(etapas, eventos, auditoria, valorFinalLisura);
                }
                return etapas;
            }

            entradaLisura = resposta.getDataEvento();
            posicao = retorno;
        }
    }

    private static void adicionarAuditagem(List<EtapaFluxo> etapas, List<FaturaEvento> eventos, int envio,
                                           BigDecimal valorFinalLisura) {
        FaturaEvento entrada = eventos.get(envio);
        int visualizacao = indiceDe(eventos, envio + 1, TipoFaturaEvento.VISUALIZADA);
        FaturaEvento saida = visualizacao < 0 ? null : eventos.get(visualizacao);

        etapas.add(new EtapaFluxo(AUDITAGEM, SETOR_AUDITORIA, entrada.getDataEvento(),
                saida == null ? null : saida.getDataEvento(),
                saida == null ? null : saida.getResponsavel(),
                valorFinalLisura, valorFinalLisura));
        if (saida != null) {
            etapas.add(new EtapaFluxo(ARQUIVO, SETOR_FATURAS, saida.getDataEvento(), null, null,
                    valorFinalLisura, valorFinalLisura));
        }
    }

    private static int indiceDe(List<FaturaEvento> eventos, int desde, TipoFaturaEvento... tipos) {
        for (int i = desde; i < eventos.size(); i++) {
            for (TipoFaturaEvento tipo : tipos) {
                if (eventos.get(i).getTipo() == tipo) {
                    return i;
                }
            }
        }
        return -1;
    }
}