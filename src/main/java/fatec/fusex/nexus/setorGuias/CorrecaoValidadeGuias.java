package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;

/**
 * T16-BE3: correcao das Guias concluidas antes da validade ser conferida. Relê o PDF guardado de cada Guia sem
 * validade (com o mesmo leitor da T16-BE1) e grava a data de NAO RECEBER APOS. Guia cujo PDF nao traz a data
 * fica sem validade e e registrada no log (id e numero), para a equipe corrigir a mao.
 *
 * Idempotente: Guia corrigida sai do filtro e nao e relida. Depois que nenhuma Guia estiver sem validade,
 * a execucao nao faz nada.
 */
@Service
public class CorrecaoValidadeGuias {

    private static final Logger LOG = LoggerFactory.getLogger(CorrecaoValidadeGuias.class);

    private final GuiaRepository guiaRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final TransactionTemplate transacao;

    public CorrecaoValidadeGuias(GuiaRepository guiaRepository,
                                 ArmazenamentoArquivo armazenamentoArquivo,
                                 PlatformTransactionManager transactionManager) {
        this.guiaRepository = guiaRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    public Resultado executar() {
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
        LOG.info("Correcao de validade das Guias: {} corrigida(s), {} sem data no PDF.", corrigidas, semDataNoPdf);
        return new Resultado(corrigidas, semDataNoPdf);
    }

    // Um PDF ilegivel ou um arquivo ausente nao derruba a correcao das outras Guias: vira "sem data" no log
    private Optional<LocalDate> lerValidade(Guia guia) {
        try {
            byte[] conteudo = armazenamentoArquivo.carregar(guia.getArquivoUrl()).getInputStream().readAllBytes();
            return LeitorGuiaPdf.extrairValidade(LeitorGuiaPdf.lerTexto(conteudo)).map(LocalDate::parse);
        } catch (IOException | RuntimeException e) {
            LOG.warn("Nao foi possivel ler o PDF da Guia (id={}, numero={}): {}",
                    guia.getIdGuia(), guia.getNumero(), e.getMessage());
            return Optional.empty();
        }
    }

    public record Resultado(int corrigidas, int semDataNoPdf) {
    }
}
