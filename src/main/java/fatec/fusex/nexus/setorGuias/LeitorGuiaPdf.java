package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.setorGuias.dto.ExameLidoResponse;

final class LeitorGuiaPdf {

    private static final Pattern PACIENTE = Pattern.compile("\\(PACIENTE\\)");
    private static final Pattern PREC_CP = Pattern.compile("Prec\\s*CP\\s*:\\s*([\\d.\\- ]*\\d)");
    private static final Pattern INICIO_PROCEDIMENTOS = Pattern.compile("Procedimentos\\s*:");
    private static final Pattern FIM_PROCEDIMENTOS = Pattern.compile("Cotista|Obs\\s*\\.\\s*:");
    private static final Pattern LINHA_EXAME = Pattern.compile("^(.*?\\p{L}.*?)\\s+(\\d{4,})(?!\\d)(.*)$");
    private static final Pattern QUANTIDADE = Pattern.compile("^\\s+(\\d{1,4})(?![\\d.,])");

    // T16-BE1: UG e UG Origem sao lidas linha a linha. Antes do rotulo pode haver caractere de borda
    // (ex.: "< UG: Cmdo Bda Inf Amv"). "UG:" nao casa com "UG Origem:", porque ha espaco antes do ":".
    private static final Pattern UG_LINHA = Pattern.compile("(?iu)^[^\\p{L}\\p{N}]*UG:\\s*(.*)$");
    private static final Pattern UG_ORIGEM_LINHA = Pattern.compile("(?iu)^[^\\p{L}\\p{N}]*UG\\s+Origem:\\s*(.*)$");
    // Rotulos de outros campos do cabecalho e do corpo da Guia: a linha seguinte a UG Origem nao e continuacao se comecar com um deles.
    private static final Pattern ROTULO_DE_CAMPO = Pattern.compile(
            "(?iu)^[^\\p{L}\\p{N}]*(UG|Nr|N[ÃA]O\\s+RECEBER|Prec|Assinatura|Titular|Benefici|GUIA|Data:|Validade|Operador|"
                    + "Procedimentos|Cod|Prest|Endere|Contato|Prof|Cotista|Obs)");

    // T16-BE1: validade (com UNICODE_CASE para acentos e maiúsculas/minúsculas)
    private static final Pattern VALIDADE_PATTERN = Pattern.compile("(?iu)N[ÃA]O\\s+RECEBER\\s+AP[ÓO]S:\\s*(\\d{2}/\\d{2}/\\d{4})");

    private LeitorGuiaPdf() {
    }

    static String lerTexto(MultipartFile pdf) {
        try {
            return lerTexto(pdf.getBytes());
        } catch (IOException e) {
            throw new DadosInvalidosException("pdf", "Nao foi possivel abrir este PDF. Envie outro arquivo.");
        }
    }

    // T16-BE3: o mesmo leitor para o PDF ja guardado (correcao das Guias antigas)
    static String lerTexto(byte[] conteudo) {
        try (PDDocument documento = Loader.loadPDF(conteudo)) {
            return new PDFTextStripper().getText(documento);
        } catch (IOException e) {
            throw new DadosInvalidosException("pdf", "Nao foi possivel abrir este PDF. Envie outro arquivo.");
        }
    }

    static Optional<String> precCpDoPaciente(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String normalizado = texto.replaceAll("\\s+", " ");
        Matcher paciente = PACIENTE.matcher(normalizado);
        if (!paciente.find()) {
            return Optional.empty();
        }
        Matcher precCp = PREC_CP.matcher(normalizado);
        precCp.region(paciente.end(), normalizado.length());
        return precCp.find() ? Optional.of(somenteDigitos(precCp.group(1))) : Optional.empty();
    }

    static List<ExameLidoResponse> examesDaGuia(String texto) {
        List<ExameLidoResponse> exames = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return exames;
        }
        boolean dentroDosProcedimentos = false;
        for (String linha : texto.split("\\R")) {
            String limpa = linha.trim();
            if (!dentroDosProcedimentos) {
                dentroDosProcedimentos = INICIO_PROCEDIMENTOS.matcher(limpa).find();
                continue;
            }
            if (FIM_PROCEDIMENTOS.matcher(limpa).find()) {
                break;
            }
            Matcher exame = LINHA_EXAME.matcher(limpa);
            if (exame.matches()) {
                exames.add(new ExameLidoResponse(
                        exame.group(2), exame.group(1).trim(), lerQuantidade(exame.group(3))));
            }
        }
        return exames;
    }

    static boolean precCpConfere(String lido, String cadastrado) {
        return somenteDigitos(lido).equals(somenteDigitos(cadastrado));
    }

    static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private static Integer lerQuantidade(String resto) {
        Matcher quantidade = QUANTIDADE.matcher(resto);
        return quantidade.find() ? Integer.valueOf(quantidade.group(1)) : null;
    }

    // ==========================================
    // T16-BE1: Novos métodos de extração
    // ==========================================

    // A linha de UG pode vir com um caractere de borda antes do rotulo (ex.: "< UG: Cmdo Bda Inf Amv").
    // A UG cabe em uma linha: nao junta a linha seguinte (o cabecalho da Guia segue logo abaixo).
    static Optional<String> extrairUg(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        for (String linha : texto.split("\\R")) {
            Matcher m = UG_LINHA.matcher(linha.trim());
            if (m.matches()) {
                String valor = m.group(1).trim();
                return valor.isEmpty() ? Optional.empty() : Optional.of(valor);
            }
        }
        return Optional.empty();
    }

    // A UG Origem pode quebrar o valor em duas linhas ("UG Origem: Cmdo" + "Bda Inf Amv").
    // Junta a linha seguinte so se ela nao for um rotulo de outro campo.
    // T16-BE1: UG do FUSEX (unica aceita). Um valor lido que e so um prefixo dela (ex.: "Cmdo", quando a
    // continuacao nao foi encontrada) e leitura incompleta: devolve vazio, e a conferencia nao bloqueia.
    static final String UG_FUSEX = "Cmdo Bda Inf Amv";

    static Optional<String> extrairUgOrigem(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String[] linhas = texto.split("\\R");
        for (int i = 0; i < linhas.length; i++) {
            Matcher m = UG_ORIGEM_LINHA.matcher(linhas[i].trim());
            if (m.matches()) {
                String valor = m.group(1).trim();
                if (i + 1 < linhas.length) {
                    String proxima = linhas[i + 1].trim();
                    if (!proxima.isEmpty() && !ROTULO_DE_CAMPO.matcher(proxima).find()) {
                        valor = (valor + " " + proxima).trim();
                    }
                }
                if (valor.isEmpty() || ehPrefixoIncompletoDaUgFusex(valor)) {
                    return Optional.empty();
                }
                return Optional.of(valor);
            }
        }
        return Optional.empty();
    }

    private static boolean ehPrefixoIncompletoDaUgFusex(String valor) {
        return valor.length() < UG_FUSEX.length() && UG_FUSEX.startsWith(valor);
    }

    static Optional<String> extrairValidade(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        Matcher m = VALIDADE_PATTERN.matcher(texto);
        if (m.find()) {
            String dataStr = m.group(1);
            try {
                // T16-BE1: ResolverStyle.STRICT evita que 31/02 vire 28/02
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
                LocalDate data = LocalDate.parse(dataStr, formatter);
                return Optional.of(data.format(DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}