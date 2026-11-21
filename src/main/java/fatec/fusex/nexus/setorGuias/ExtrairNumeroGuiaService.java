package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.setorGuias.dto.ExtracaoNumeroGuiaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T11-BE1 estendido pela T21-BE1 e T16-BE1: POST /setor-guias/extrair-numero-guia. Le o numero, o Prec CP do paciente,
 * os exames, a UG, a UG Origem e a validade da Guia no PDF anexado, sem gravar nada.
 */
@Service
public class ExtrairNumeroGuiaService {

    private static final byte[] ASSINATURA_PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final Pattern CABECALHO = Pattern.compile("GUIA DE ENCAMINHAMENTO", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERO = Pattern.compile("Nr:\\s*(\\d{1,20})(?!\\d)");

    private final UsuarioLogado usuarioLogado;
    private final LimiteLeituraGuia limiteLeituraGuia;
    private final PreGuiaRepository preGuiaRepository;
    private final long tamanhoMaximoBytes;

    public ExtrairNumeroGuiaService(UsuarioLogado usuarioLogado,
                                    LimiteLeituraGuia limiteLeituraGuia,
                                    PreGuiaRepository preGuiaRepository,
                                    @Value("${nexus.guia.pdf-tamanho-maximo-bytes:10485760}") long tamanhoMaximoBytes) {
        this.usuarioLogado = usuarioLogado;
        this.limiteLeituraGuia = limiteLeituraGuia;
        this.preGuiaRepository = preGuiaRepository;
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }

    public ExtracaoNumeroGuiaResponse extrair(MultipartFile pdf, Long preGuiaId) {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);
        limiteLeituraGuia.registrar(usuario.getLogin());

        validarPdf(pdf);
        String texto = LeitorGuiaPdf.lerTexto(pdf);

        String numero = extrairNumeroDoTexto(texto).orElseThrow(() -> new DadosInvalidosException(
                "pdf", "Nao foi possivel ler o numero da Guia neste PDF. Digite o numero manualmente."));

        String precCpCadastrado = precCpDaPreGuia(preGuiaId);
        String precCpLido = LeitorGuiaPdf.precCpDoPaciente(texto).orElse(null);
        Boolean precCpConfere = precCpLido == null || precCpCadastrado == null
                ? null
                : LeitorGuiaPdf.precCpConfere(precCpLido, precCpCadastrado);

        // T16-BE1: Leitura de UG, UG Origem e Validade
        String ug = LeitorGuiaPdf.extrairUg(texto).orElse(null);
        String ugOrigem = LeitorGuiaPdf.extrairUgOrigem(texto).orElse(null);
        String validadeAte = LeitorGuiaPdf.extrairValidade(texto).orElse(null);

        return new ExtracaoNumeroGuiaResponse(
                numero,
                precCpLido,
                precCpConfere,
                LeitorGuiaPdf.examesDaGuia(texto),
                ug,
                ugOrigem,
                validadeAte,
                nomeDoPacienteDaPreGuia(preGuiaId)
        );
    }

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

    private String nomeDoPacienteDaPreGuia(Long preGuiaId) {
        if (preGuiaId == null) {
            return null;
        }
        return preGuiaRepository.findById(preGuiaId)
                .map(fatec.fusex.nexus.preGuia.PacienteDaPreGuia::nome)
                .orElse(null);
    }

    private String precCpDaPreGuia(Long preGuiaId) {
        if (preGuiaId == null) {
            return null;
        }
        PreGuia preGuia = preGuiaRepository.findById(preGuiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId));
        return fatec.fusex.nexus.preGuia.PacienteDaPreGuia.precCp(preGuia);
    }

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
}