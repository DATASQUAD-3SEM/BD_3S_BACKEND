package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimento;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoId;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.setorGuias.dto.GuiaRascunhoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * T4-BE5: PUT /setor-guias/pre-guias/{id}/guia-rascunho (multipart com numeroGuia, pdf e exames).
 * Grava ou substitui o rascunho da Guia emitida no SIRE, com a lista de exames conferida pelo setor (T22-BE1).
 * Nao cria a Guia nem muda o status: isso fica com o "Concluir" (T4-BE6).
 */
@Service
public class AnexarGuiaService {

    private static final String PASTA = "guias-rascunho";
    private static final int NUMERO_MAXIMO = 20; // tamanho da coluna numero (Guia e GuiaRascunho)
    private static final byte[] ASSINATURA_PDF = "%PDF".getBytes();

    // T22-BE1: limites da lista de exames (regra de aplicacao; o banco so exige codigo numerico e quantidade > 0)
    private static final int EXAMES_MINIMO = 1;
    private static final int EXAMES_MAXIMO = 50;
    private static final int DESCRICAO_MAXIMA = 500;
    private static final int QUANTIDADE_MAXIMA = 999;
    private static final Pattern COD_DGP = Pattern.compile("[0-9]{1,10}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AnexarGuiaRepository repository;
    private final GuiaRascunhoProcedimentoRepository procedimentoRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final UsuarioLogado usuarioLogado;
    private final long tamanhoMaximoBytes;

    public AnexarGuiaService(AnexarGuiaRepository repository,
                             GuiaRascunhoProcedimentoRepository procedimentoRepository,
                             ArmazenamentoArquivo armazenamentoArquivo,
                             UsuarioLogado usuarioLogado,
                             @Value("${nexus.guia.pdf-tamanho-maximo-bytes:10485760}") long tamanhoMaximoBytes) {
        this.repository = repository;
        this.procedimentoRepository = procedimentoRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.usuarioLogado = usuarioLogado;
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }

    // Ordem: perfil (403) -> existe (404) -> numero, arquivo e exames (422) -> status/Guia concluida (409) -> numero repetido (409)
    @Transactional
    public GuiaRascunhoResponse anexar(Long id, String numeroGuia, MultipartFile pdf, String exames) {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

        PreGuia preGuia = repository.buscarPreGuiaParaAnexar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + id));

        String numero = validarNumero(numeroGuia);
        validarPdf(pdf);
        List<GuiaRascunhoProcedimento> linhasExames = validarExames(exames, id);

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

            // A lista nova substitui a anterior (mesma transacao: ou grava tudo, ou nada).
            // Remove por entidade e faz flush antes de inserir, para o contexto nao guardar linhas apagadas.
            procedimentoRepository.deleteAll(procedimentoRepository.listarDaPreGuia(id));
            procedimentoRepository.flush();
            procedimentoRepository.saveAllAndFlush(linhasExames);
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

    // T22-BE1: JSON com 1 a 50 exames { codDgp, descricao, quantidade }. codDgp so digitos (1 a 10) e sem repetir,
    // descricao de 1 a 500 caracteres sem espacos nas pontas, quantidade inteira de 1 a 999.
    static List<GuiaRascunhoProcedimento> validarExames(String exames, Long idPreGuia) {
        if (exames == null || exames.isBlank()) {
            throw erroExames("Informe os exames da Guia");
        }
        JsonNode lista;
        try {
            lista = JSON.readTree(exames);
        } catch (JacksonException e) {
            throw erroExames("A lista de exames deve estar em JSON");
        }
        if (lista == null || !lista.isArray()) {
            throw erroExames("A lista de exames deve ser um array JSON");
        }
        if (lista.size() < EXAMES_MINIMO || lista.size() > EXAMES_MAXIMO) {
            throw erroExames("Informe de " + EXAMES_MINIMO + " a " + EXAMES_MAXIMO + " exames");
        }

        Set<String> codigos = new HashSet<>();
        List<GuiaRascunhoProcedimento> linhas = new ArrayList<>();
        int ordem = 1;
        for (JsonNode item : lista) {
            if (!item.isObject()) {
                throw erroExames("Cada exame deve ser um objeto com codDgp, descricao e quantidade");
            }

            String codDgp = textoDe(item, "codDgp");
            if (codDgp == null || !COD_DGP.matcher(codDgp).matches()) {
                throw erroExames("Cod DGP deve ter so digitos, de 1 a 10");
            }
            if (!codigos.add(codDgp)) {
                throw erroExames("Cod DGP repetido na lista: " + codDgp);
            }

            String descricao = textoDe(item, "descricao");
            if (descricao == null || descricao.isEmpty() || descricao.length() > DESCRICAO_MAXIMA
                    || !descricao.equals(descricao.strip())) {
                throw erroExames("A descricao deve ter de 1 a " + DESCRICAO_MAXIMA
                        + " caracteres, sem espacos no inicio ou no fim");
            }

            JsonNode quantidade = item.get("quantidade");
            if (quantidade == null || !quantidade.isInt()
                    || quantidade.intValue() < 1 || quantidade.intValue() > QUANTIDADE_MAXIMA) {
                throw erroExames("A quantidade deve ser um inteiro de 1 a " + QUANTIDADE_MAXIMA);
            }

            linhas.add(new GuiaRascunhoProcedimento(
                    new GuiaRascunhoProcedimentoId(idPreGuia, ordem++),
                    codDgp,
                    descricao,
                    quantidade.intValue()));
        }
        return linhas;
    }

    private static String textoDe(JsonNode item, String campo) {
        JsonNode no = item.get(campo);
        return no != null && no.isString() ? no.textValue() : null;
    }

    private static DadosInvalidosException erroExames(String mensagem) {
        return new DadosInvalidosException("exames", mensagem);
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
