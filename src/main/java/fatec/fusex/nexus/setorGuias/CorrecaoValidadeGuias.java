package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.common.rotina.RotinaExecucao;
import fatec.fusex.nexus.common.rotina.RotinaExecucaoRepository;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;

/**
 * T16-BE3: correcao unica das Guias concluidas antes da validade ser conferida. Relê o PDF guardado de cada Guia
 * sem validade (com o mesmo leitor da T16-BE1) e grava a data de NAO RECEBER APOS. Guia cujo PDF nao traz a data
 * fica sem validade e e registrada no log (id e numero), para a equipe corrigir a mao.
 *
 * Execucao unica: ao terminar, grava a linha da rotina em RotinaExecucao. Nas execucoes seguintes, a linha
 * existe e a correcao nao roda de novo. Para rodar de novo, apagar a linha da rotina no banco.
 */
@Service
public class CorrecaoValidadeGuias {

    static final String NOME_ROTINA = "CORRECAO_VALIDADE_GUIAS_T16_BE3";

    private static final Logger LOG = LoggerFactory.getLogger(CorrecaoValidadeGuias.class);

    private final GuiaRepository guiaRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final RotinaExecucaoRepository rotinaExecucaoRepository;
    private final TransactionTemplate transacao;

    public CorrecaoValidadeGuias(GuiaRepository guiaRepository,
                                 ArmazenamentoArquivo armazenamentoArquivo,
                                 RotinaExecucaoRepository rotinaExecucaoRepository,
                                 PlatformTransactionManager transactionManager) {
        this.guiaRepository = guiaRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.rotinaExecucaoRepository = rotinaExecucaoRepository;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    /** Devolve o resultado da correcao, ou vazio se a rotina ja tinha rodado antes. */
    public Optional<Resultado> executar() {
        if (rotinaExecucaoRepository.existsById(NOME_ROTINA)) {
            return Optional.empty();
        }
        int corrigidas = 0;
        int semDataNoPdf = 0;
        for (Guia guia : guiaRepository.findByDataValidadeAteIsNull()) {
            Optional<LocalDate> validade = lerValidade(guia);
            if (validade.isEmpty()) {
                semDataNoPdf++;
                LOG.warn("Guia sem validade: o PDF nao traz NAO RECEBER APOS (id={}, numero={}). Corrigir a mao.",
                        guia.getIdGuia(), guia.getNumero());
                continue;
            }
            Long idGuia = guia.getIdGuia();
            LocalDate data = validade.get();
            transacao.executeWithoutResult(status -> guiaRepository.findById(idGuia)
                    .filter(atual -> atual.getDataValidadeAte() == null)
                    .ifPresent(atual -> atual.setDataValidadeAte(data)));
            corrigidas++;
        }
        transacao.executeWithoutResult(status -> rotinaExecucaoRepository.save(
                new RotinaExecucao(NOME_ROTINA, LocalDateTime.now())));
        LOG.info("Correcao de validade das Guias: {} corrigida(s), {} sem data no PDF.", corrigidas, semDataNoPdf);
        return Optional.of(new Resultado(corrigidas, semDataNoPdf));
    }

    /** Data de NAO RECEBER APOS do PDF, ou vazio se o PDF nao traz a data. Usado pela correcao e pela migration V13. */
    public static Optional<LocalDate> validadeDoPdf(byte[] conteudo) {
        return LeitorGuiaPdf.extrairValidade(LeitorGuiaPdf.lerTexto(conteudo)).map(LocalDate::parse);
    }

    // Um PDF ilegivel ou um arquivo ausente nao derruba a correcao das outras Guias: vira "sem data" no log
    private Optional<LocalDate> lerValidade(Guia guia) {
        try {
            byte[] conteudo = armazenamentoArquivo.carregar(guia.getArquivoUrl()).getInputStream().readAllBytes();
            return validadeDoPdf(conteudo);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Nao foi possivel ler o PDF da Guia (id={}, numero={}): {}",
                    guia.getIdGuia(), guia.getNumero(), e.getMessage());
            return Optional.empty();
        }
    }

    public record Resultado(int corrigidas, int semDataNoPdf) {
    }
}
