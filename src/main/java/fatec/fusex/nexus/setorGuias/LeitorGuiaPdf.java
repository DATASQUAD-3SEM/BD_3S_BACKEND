package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    // T16-BE1: Padrões para UG, UG Origem e Validade
    private static final Pattern UG_PATTERN = Pattern.compile("(?i)UG:\\s*(.*?)(?=\\s*UG\\s*Origem:|\\s*Nr:|\\s*N[ÃA]O\\s+RECEBER|$)");
    private static final Pattern UG_ORIGEM_PATTERN = Pattern.compile("(?i)UG\\s*Origem:\\s*(.*?)(?=\\s*UG:|\\s*Nr:|\\s*N[ÃA]O\\s+RECEBER|$)");
    private static final Pattern VALIDADE_PATTERN = Pattern.compile("(?i)N[ÃA]O\\s+RECEBER\\s+AP[ÓO]S:\\s*(\\d{2}/\\d{2}/\\d{4})");

    private LeitorGuiaPdf() {
    }

    static String lerTexto(MultipartFile pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf.getBytes())) {
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

    static Optional<String> extrairUg(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String normalizado = texto.replaceAll("\\s+", " ");
        Matcher m = UG_PATTERN.matcher(normalizado);
        if (m.find()) {
            String valor = m.group(1).trim();
            return valor.isEmpty() ? Optional.empty() : Optional.of(valor);
        }
        return Optional.empty();
    }

    static Optional<String> extrairUgOrigem(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String normalizado = texto.replaceAll("\\s+", " ");
        Matcher m = UG_ORIGEM_PATTERN.matcher(normalizado);
        if (m.find()) {
            String valor = m.group(1).trim();
            return valor.isEmpty() ? Optional.empty() : Optional.of(valor);
        }
        return Optional.empty();
    }

    static Optional<String> extrairValidade(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String normalizado = texto.replaceAll("\\s+", " ");
        Matcher m = VALIDADE_PATTERN.matcher(normalizado);
        if (m.find()) {
            String dataStr = m.group(1);
            try {
                LocalDate data = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return Optional.of(data.format(DateTimeFormatter.ISO_LOCAL_DATE)); // Retorna yyyy-mm-dd
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}