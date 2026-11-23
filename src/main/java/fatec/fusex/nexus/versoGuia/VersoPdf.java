package fatec.fusex.nexus.versoGuia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import fatec.fusex.nexus.andamentoGlosa.dto.AndamentoGlosaResponse.ItemAndamento;

/**
 * Gera o PDF dos versos (T32-BE1, regra 15): A4 retrato, preto e branco, sem campo de assinatura. Cada Guia comeca
 * numa pagina nova; se o andamento nao couber, continua na pagina seguinte com a Guia repetida no rodape.
 */
@Component
public class VersoPdf {

    private static final float MARGEM = 50;
    private static final float LARGURA = PDRectangle.A4.getWidth() - 2 * MARGEM;
    private static final float RODAPE = 40;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final float[] COLUNAS = {0, 60, 260, 300, 370, 440};

    public byte[] gerar(List<VersoGuia> versos, LocalDate emissao) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            for (VersoGuia verso : versos) {
                new Pagina(documento, verso, emissao).escrever();
            }
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o PDF do verso", e);
        }
    }

    /** Escreve um verso, abrindo paginas novas quando o texto chega ao rodape. */
    private static final class Pagina {

        private final PDDocument documento;
        private final VersoGuia verso;
        private final LocalDate emissao;
        private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream conteudo;
        private float y;

        Pagina(PDDocument documento, VersoGuia verso, LocalDate emissao) {
            this.documento = documento;
            this.verso = verso;
            this.emissao = emissao;
        }

        void escrever() throws IOException {
            novaPagina();
            linha("Guia nº " + verso.guiaNumero() + "   ·   Fatura nº " + valor(verso.numeroFatura()), negrito, 15);
            y -= 6;
            linha("Paciente: " + verso.paciente(), normal, 10);
            if (verso.titular() != null) {
                linha("Titular: " + verso.titular(), normal, 10);
            }
            linha("OCS: " + verso.ocs() + "   ·   Contrato: " + valor(verso.contrato()), normal, 10);
            y -= 8;

            linha("Procedimentos glosados", negrito, 11);
            cabecalhoTabela();
            for (VersoGuia.Linha l : verso.linhas()) {
                garantirEspaco(14);
                celulas(normal, 9, l.codigo(), cortar(l.procedimento(), 195, 9), String.valueOf(l.quantidade()),
                        reais(l.informado()), reais(l.contrato()), glosa(l.glosa()));
            }
            garantirEspaco(14);
            celulas(negrito, 9, "", "Total da Guia", "", "", "", glosa(verso.totalGlosa()));
            y -= 8;

            if (!verso.observacoes().isEmpty()) {
                linha("Observação da rastreabilidade", negrito, 11);
                for (String observacao : verso.observacoes()) {
                    paragrafo(observacao, normal, 10);
                }
                y -= 8;
            }

            linha("Andamento da glosa · " + verso.andamento().seloFinal(), negrito, 11);
            for (ItemAndamento item : verso.andamento().itens()) {
                paragrafo(item.data().toLocalDate().format(DATA) + " — " + item.texto(), normal, 10);
            }
            conteudo.close();
        }

        private void novaPagina() throws IOException {
            if (conteudo != null) {
                conteudo.close();
            }
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            conteudo = new PDPageContentStream(documento, pagina);
            texto("Emitido em " + emissao.format(DATA) + "   ·   Imprimir no verso da Guia nº " + verso.guiaNumero(),
                    normal, 8, MARGEM, RODAPE - 15);
            y = PDRectangle.A4.getHeight() - MARGEM;
        }

        private void garantirEspaco(float altura) throws IOException {
            if (y - altura < RODAPE) {
                novaPagina();
            }
        }

        private void cabecalhoTabela() throws IOException {
            garantirEspaco(14);
            celulas(negrito, 9, "Código", "Procedimento", "Qtd.", "Informado", "Contrato", "Glosa");
        }

        private void celulas(PDType1Font fonte, float tamanho, String... valores) throws IOException {
            y -= tamanho + 4;
            for (int i = 0; i < valores.length; i++) {
                texto(valores[i], fonte, tamanho, MARGEM + COLUNAS[i], y);
            }
        }

        private void linha(String texto, PDType1Font fonte, float tamanho) throws IOException {
            garantirEspaco(tamanho + 4);
            y -= tamanho + 4;
            texto(texto, fonte, tamanho, MARGEM, y);
        }

        private void paragrafo(String texto, PDType1Font fonte, float tamanho) throws IOException {
            for (String parte : quebrar(seguro(texto, fonte), fonte, tamanho)) {
                linha(parte, fonte, tamanho);
            }
        }

        private List<String> quebrar(String texto, PDType1Font fonte, float tamanho) throws IOException {
            List<String> linhas = new ArrayList<>();
            StringBuilder atual = new StringBuilder();
            for (String palavra : texto.split(" ")) {
                String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
                if (fonte.getStringWidth(tentativa) / 1000 * tamanho > LARGURA && !atual.isEmpty()) {
                    linhas.add(atual.toString());
                    atual = new StringBuilder(palavra);
                } else {
                    atual = new StringBuilder(tentativa);
                }
            }
            if (!atual.isEmpty()) {
                linhas.add(atual.toString());
            }
            return linhas;
        }

        private String cortar(String texto, float largura, float tamanho) throws IOException {
            String seguro = seguro(valor(texto), normal);
            if (normal.getStringWidth(seguro) / 1000 * tamanho <= largura) {
                return seguro;
            }
            String corte = seguro;
            while (!corte.isEmpty() && normal.getStringWidth(corte + "...") / 1000 * tamanho > largura) {
                corte = corte.substring(0, corte.length() - 1);
            }
            return corte + "...";
        }

        private void texto(String texto, PDType1Font fonte, float tamanho, float x, float yTexto) throws IOException {
            conteudo.beginText();
            conteudo.setFont(fonte, tamanho);
            conteudo.newLineAtOffset(x, yTexto);
            conteudo.showText(seguro(texto, fonte));
            conteudo.endText();
        }

        /** Troca por "?" o caractere que a fonte padrao do PDF nao consegue escrever (ex.: emoji no motivo). */
        private static String seguro(String texto, PDType1Font fonte) {
            StringBuilder resultado = new StringBuilder();
            texto.codePoints().forEach(c -> {
                String caractere = new String(Character.toChars(c));
                try {
                    fonte.encode(caractere);
                    resultado.append(caractere);
                } catch (IOException | IllegalArgumentException e) {
                    resultado.append('?');
                }
            });
            return resultado.toString();
        }
    }

    private static String valor(String texto) {
        return texto == null ? "—" : texto;
    }

    private static String reais(BigDecimal valor) {
        return valor == null ? "—" : String.format(PT_BR, "R$ %,.2f", valor);
    }

    private static String glosa(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }
        String sinal = valor.signum() > 0 ? "- " : valor.signum() < 0 ? "+ " : "";
        return sinal + reais(valor.abs());
    }
}
