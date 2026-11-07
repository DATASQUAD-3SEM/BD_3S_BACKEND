package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.setorGuias.dto.ExtracaoNumeroGuiaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T11-BE1: POST /setor-guias/extrair-numero-guia. Le o numero da Guia no PDF anexado, sem gravar nada.
 * A gravacao acontece depois, no PUT do "Concluir" (T4-BE5).
 *
 * Le so o PDF com texto selecionavel. Um PDF escaneado cai no 422, e a leitura de escaneados fica fora desta versao.
 */
@Service
public class ExtrairNumeroGuiaService {

    private static final byte[] ASSINATURA_PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final Pattern CABECALHO = Pattern.compile("GUIA DE ENCAMINHAMENTO", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERO = Pattern.compile("Nr:\\s*(\\d{1,20})(?!\\d)");

    private final UsuarioLogado usuarioLogado;
    private final LimiteLeituraGuia limiteLeituraGuia;
    private final long tamanhoMaximoBytes;

    public ExtrairNumeroGuiaService(UsuarioLogado usuarioLogado,
                                    LimiteLeituraGuia limiteLeituraGuia,
                                    @Value("${nexus.guia.pdf-tamanho-maximo-bytes:10485760}") long tamanhoMaximoBytes) {
        this.usuarioLogado = usuarioLogado;
        this.limiteLeituraGuia = limiteLeituraGuia;
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }

    // Ordem: perfil (403) -> limite de leituras (429) -> arquivo (400) -> numero no PDF (422)
    public ExtracaoNumeroGuiaResponse extrair(MultipartFile pdf) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);
        limiteLeituraGuia.registrar(usuario.getLogin());

        validarPdf(pdf);
        String texto = lerTexto(pdf);

        String numero = extrairNumeroDoTexto(texto).orElseThrow(() -> new DadosInvalidosException(
                "pdf", "Nao foi possivel ler o numero da Guia neste PDF. Digite o numero manualmente."));
        return new ExtracaoNumeroGuiaResponse(numero);
    }

    /**
     * Procura "Nr: <digitos>" depois do cabecalho "GUIA DE ENCAMINHAMENTO". Sem o cabecalho ou sem o padrao, devolve vazio.
     * Espacos e quebras de linha sao unificados antes, porque o extrator do PDF pode quebrar linhas no meio.
     */
    static Optional<String> extrairNumeroDoTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String normalizado = texto.replaceAll("\\s+", " ");

        Matcher cabecalho = CABECALHO.matcher(normalizado);
        if (!cabecalho.find()) {
            return Optional.empty();
        }

        Matcher numero = NUMERO.matcher(normalizado);
        numero.region(cabecalho.end(), normalizado.length());
        return numero.find() ? Optional.of(numero.group(1)) : Optional.empty();
    }

    // So PDF (extensao e conteudo comecando com %PDF) e ate o limite configurado
    private void validarPdf(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new RegraDeNegocioException("Anexe o PDF da Guia");
        }
        String nome = pdf.getOriginalFilename() == null ? "" : pdf.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!nome.endsWith(".pdf") || !comecaComPdf(pdf)) {
            throw new RegraDeNegocioException("O arquivo da Guia deve ser um PDF");
        }
        if (pdf.getSize() > tamanhoMaximoBytes) {
            throw new RegraDeNegocioException(
                    "O PDF deve ter no maximo " + (tamanhoMaximoBytes / (1024 * 1024)) + " MB");
        }
    }

    private static boolean comecaComPdf(MultipartFile pdf) {
        try {
            byte[] inicio = pdf.getInputStream().readNBytes(ASSINATURA_PDF.length);
            return Arrays.equals(inicio, ASSINATURA_PDF);
        } catch (IOException e) {
            return false;
        }
    }

    private static String lerTexto(MultipartFile pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf.getBytes())) {
            return new PDFTextStripper().getText(documento);
        } catch (IOException e) {
            throw new DadosInvalidosException("pdf", "Nao foi possivel abrir este PDF. Envie outro arquivo.");
        }
    }
}
