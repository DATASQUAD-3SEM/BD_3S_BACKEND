package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.setorGuias.dto.GuiaRascunhoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;

/**
 * T4-BE5: PUT /setor-guias/pre-guias/{id}/guia-rascunho (multipart com numeroGuia e pdf).
 * Grava ou substitui o rascunho da Guia emitida no SIRE. Nao cria a Guia nem muda o status:
 * isso fica com o "Concluir" (T4-BE6).
 */
@Service
public class AnexarGuiaService {

    private static final String PASTA = "guias-rascunho";
    private static final int NUMERO_MAXIMO = 20; // tamanho da coluna numero (Guia e GuiaRascunho)
    private static final byte[] ASSINATURA_PDF = "%PDF".getBytes();

    private final AnexarGuiaRepository repository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final UsuarioLogado usuarioLogado;
    private final long tamanhoMaximoBytes;

    public AnexarGuiaService(AnexarGuiaRepository repository,
                             ArmazenamentoArquivo armazenamentoArquivo,
                             UsuarioLogado usuarioLogado,
                             @Value("${nexus.guia.pdf-tamanho-maximo-bytes:10485760}") long tamanhoMaximoBytes) {
        this.repository = repository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.usuarioLogado = usuarioLogado;
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }

    // Ordem: perfil (403) -> existe (404) -> numero e arquivo (422) -> status/Guia concluida (409) -> numero repetido (409)
    @Transactional
    public GuiaRascunhoResponse anexar(Long id, String numeroGuia, MultipartFile pdf) {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

        PreGuia preGuia = repository.buscarPreGuiaParaAnexar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + id));

        String numero = validarNumero(numeroGuia);
        validarPdf(pdf);

        if (preGuia.getStatus() != PreGuiaStatus.EM_ANALISE) {
            throw new ConflitoException("A pre-guia nao esta Em analise: ela ja foi alterada");
        }
        if (repository.temGuiaConcluida(id)) {
            throw new ConflitoException("A Guia desta pre-guia ja foi concluida");
        }
        if (repository.numeroUsadoEmGuia(numero) || repository.numeroUsadoEmOutroRascunho(numero, id)) {
            throw new ConflitoException("numeroGuia", "Numero de Guia ja usado em outra Guia");
        }

        GuiaRascunho rascunho = repository.findById(id).orElse(null);
        String arquivoAnterior = rascunho == null ? null : rascunho.getArquivoUrl();

        String arquivoNovo = armazenamentoArquivo.salvar(pdf, PASTA);
        try {
            if (rascunho == null) {
                rascunho = new GuiaRascunho();
                rascunho.setPreGuia(preGuia);
            }
            rascunho.setNumero(numero);
            rascunho.setArquivoUrl(arquivoNovo);
            rascunho.setNomeArquivo(nomeDoArquivo(pdf));
            rascunho.setDataAtualizacao(LocalDateTime.now());
            repository.saveAndFlush(rascunho);
        } catch (RuntimeException e) {
            armazenamentoArquivo.removerSemFalhar(arquivoNovo); // nao deixa arquivo orfao se o banco falhar
            throw e;
        }

        // "Trocar": o arquivo anterior e descartado
        if (arquivoAnterior != null) {
            armazenamentoArquivo.removerSemFalhar(arquivoAnterior);
        }
        return new GuiaRascunhoResponse(rascunho.getNumero(), rascunho.getNomeArquivo());
    }

    // Numerico, nao vazio e com no maximo 20 digitos
    static String validarNumero(String numeroGuia) {
        if (numeroGuia == null || numeroGuia.isBlank()) {
            throw new DadosInvalidosException("numeroGuia", "Informe o numero da Guia");
        }
        String numero = numeroGuia.trim();
        if (!numero.matches("[0-9]+")) {
            throw new DadosInvalidosException("numeroGuia", "O numero da Guia deve ter so digitos");
        }
        if (numero.length() > NUMERO_MAXIMO) {
            throw new DadosInvalidosException("numeroGuia", "O numero da Guia deve ter no maximo " + NUMERO_MAXIMO + " digitos");
        }
        return numero;
    }

    // So PDF (extensao e conteudo comecando com %PDF) e ate o limite configurado
    private void validarPdf(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new DadosInvalidosException("pdf", "Anexe o PDF da Guia");
        }
        String nome = pdf.getOriginalFilename() == null ? "" : pdf.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!nome.endsWith(".pdf") || !comecaComPdf(pdf)) {
            throw new DadosInvalidosException("pdf", "O arquivo da Guia deve ser um PDF");
        }
        if (pdf.getSize() > tamanhoMaximoBytes) {
            throw new DadosInvalidosException("pdf",
                    "O PDF deve ter no maximo " + (tamanhoMaximoBytes / (1024 * 1024)) + " MB");
        }
    }

    private static boolean comecaComPdf(MultipartFile pdf) {
        try (InputStream entrada = pdf.getInputStream()) {
            byte[] inicio = entrada.readNBytes(ASSINATURA_PDF.length);
            return Arrays.equals(inicio, ASSINATURA_PDF);
        } catch (IOException e) {
            return false;
        }
    }

    // So o nome do arquivo, sem pastas (o caminho no servidor nunca e exposto)
    private static String nomeDoArquivo(MultipartFile pdf) {
        String original = pdf.getOriginalFilename() == null ? "guia.pdf" : pdf.getOriginalFilename();
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).trim();
        if (nome.isEmpty()) {
            nome = "guia.pdf";
        }
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }
}
