package fatec.fusex.nexus.MinhasPreGuias;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.MinhasPreGuias.dto.ReenvioPreGuiaResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T9-BE4: POST /beneficiarios/me/pre-guias/{id}/reenvio (multipart com ocsId e/ou arquivo).
 * A mesma pre-guia volta para Pendente. O novo arquivo vira o encaminhamento atual e o anterior fica guardado.
 * Regra 11 do modelo: so o beneficiario titular corrige pre-guia criada por ele (medico_id nulo).
 */
@Service
public class ReenvioPreGuiaService {

    private static final String PASTA = "encaminhamentos";
    private static final int OBSERVACAO_MAXIMO = 500; // tamanho da coluna PreGuiaHistorico.observacao

    private static final byte[] ASSINATURA_PDF = "%PDF".getBytes();
    private static final byte[] ASSINATURA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final ReenvioPreGuiaRepository preGuiaRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final SimulacaoLogin simulacaoLogin;
    private final long tamanhoMaximoBytes;

    public ReenvioPreGuiaService(ReenvioPreGuiaRepository preGuiaRepository,
                                 OcsRepository ocsRepository,
                                 PreGuiaHistoricoRepository historicoRepository,
                                 UsuarioRepository usuarioRepository,
                                 ArmazenamentoArquivo armazenamentoArquivo,
                                 SimulacaoLogin simulacaoLogin,
                                 @Value("${nexus.encaminhamento.tamanho-maximo-bytes:10485760}") long tamanhoMaximoBytes) {
        this.preGuiaRepository = preGuiaRepository;
        this.ocsRepository = ocsRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.simulacaoLogin = simulacaoLogin;
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }

    // Ordem: perfil (403) -> de outro ou inexistente (404) -> origem medico (403) -> status (409) -> corpo (422)
    @Transactional
    public ReenvioPreGuiaResponse reenviar(Long id, Long ocsId, MultipartFile arquivo) {
        Usuario beneficiario = validarBeneficiario();

        // Trava a linha ate o fim da transacao (dois reenvios simultaneos geram uma unica mudanca)
        PreGuia preGuia = preGuiaRepository.buscarParaReenviar(id)
                .filter(p -> p.getBeneficiario().getUsuarioId().equals(beneficiario.getIdUsuario()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada"));

        if (preGuia.getMedicoFusex() != null) {
            throw new AcessoNegadoException("Pre-guia criada por medico nao pode ser corrigida pelo beneficiario");
        }
        if (preGuia.getStatus() != PreGuiaStatus.ATENCAO) {
            throw new ConflitoException("A pre-guia nao esta em Atencao: ela ja foi alterada");
        }

        boolean temArquivo = arquivo != null && !arquivo.isEmpty();
        if (ocsId == null && !temArquivo) {
            throw new DadosInvalidosException("arquivo", "Informe uma nova OCS e/ou um novo encaminhamento");
        }
        if (temArquivo) {
            validarArquivo(arquivo);
        }
        Ocs ocsNova = ocsId == null ? null : buscarOcsAtiva(ocsId);

        Ocs ocsAnterior = preGuia.getOcs();
        String arquivoAnterior = preGuia.getEncaminhamentoUrl();
        String observacaoDoSetor = observacaoDoSetor(preGuia);

        String arquivoNovo = temArquivo ? armazenamentoArquivo.salvar(arquivo, PASTA) : null;
        try {
            if (arquivoNovo != null) {
                preGuia.setEncaminhamentoUrl(arquivoNovo); // o anterior continua no armazenamento e no texto do historico
            }
            if (ocsNova != null) {
                preGuia.setOcs(ocsNova);
            }
            preGuia.setStatus(PreGuiaStatus.PENDENTE);

            LocalDateTime dataReenvio = LocalDateTime.now();
            // encaminhamento_url guarda o arquivo atual: e dele que a T9-BE1 tira a data de envio
            historicoRepository.saveAndFlush(new PreGuiaHistorico(
                    null, preGuia, PreGuiaStatus.PENDENTE,
                    textoDoHistorico(observacaoDoSetor, arquivoAnterior, arquivoNovo, ocsAnterior, ocsNova),
                    preGuia.getEncaminhamentoUrl(), beneficiario, dataReenvio));

            return new ReenvioPreGuiaResponse(preGuia.getStatus().name(), dataReenvio);
        } catch (RuntimeException e) {
            if (arquivoNovo != null) {
                removerSemFalhar(arquivoNovo); // nao deixa arquivo orfao se o banco falhar
            }
            throw e;
        }
    }

    // PDF, JPG ou PNG (extensao e conteudo) e ate o limite configurado
    private void validarArquivo(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        String extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1) : "";
        byte[] esperado = switch (extensao) {
            case "pdf" -> ASSINATURA_PDF;
            case "jpg", "jpeg" -> ASSINATURA_JPG;
            case "png" -> ASSINATURA_PNG;
            default -> null;
        };
        if (esperado == null || !comecaCom(arquivo, esperado)) {
            throw new DadosInvalidosException("arquivo", "Formato invalido. Envie PDF, JPG ou PNG");
        }
        if (arquivo.getSize() > tamanhoMaximoBytes) {
            throw new DadosInvalidosException("arquivo",
                    "O arquivo deve ter no maximo " + (tamanhoMaximoBytes / (1024 * 1024)) + " MB");
        }
    }

    private static boolean comecaCom(MultipartFile arquivo, byte[] esperado) {
        try (InputStream entrada = arquivo.getInputStream()) {
            return Arrays.equals(entrada.readNBytes(esperado.length), esperado);
        } catch (IOException e) {
            return false;
        }
    }

    private Ocs buscarOcsAtiva(Long ocsId) {
        return ocsRepository.findById(ocsId)
                .filter(o -> Boolean.TRUE.equals(o.getAtivo()))
                .orElseThrow(() -> new DadosInvalidosException("ocsId", "OCS inexistente ou inativa"));
    }

    private String observacaoDoSetor(PreGuia preGuia) {
        return historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                .map(PreGuiaHistorico::getObservacao)
                .orElse("-");
    }

    // O historico nao tem colunas proprias para foto e OCS antigas, entao elas vao no texto da linha do reenvio
    private static String textoDoHistorico(String observacaoDoSetor, String arquivoAnterior, String arquivoNovo,
                                           Ocs ocsAnterior, Ocs ocsNova) {
        StringBuilder texto = new StringBuilder("Reenviada pelo beneficiario.");
        if (arquivoNovo != null) {
            texto.append(" Encaminhamento: ").append(nomeDe(arquivoAnterior)).append(" -> ").append(nomeDe(arquivoNovo)).append('.');
        }
        if (ocsNova != null) {
            texto.append(" OCS: ").append(ocsAnterior.getNome()).append(" -> ").append(ocsNova.getNome()).append('.');
        }
        texto.append(" Observacao do setor: ").append(observacaoDoSetor);
        return texto.length() > OBSERVACAO_MAXIMO ? texto.substring(0, OBSERVACAO_MAXIMO) : texto.toString();
    }

    private static String nomeDe(String caminho) {
        return caminho == null ? "-" : caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    private void removerSemFalhar(String caminho) {
        try {
            armazenamentoArquivo.remover(caminho);
        } catch (RuntimeException e) {
            // a falha ao limpar o arquivo nao pode esconder a causa original
        }
    }

    private Usuario validarBeneficiario() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.BENEFICIARIO) {
            throw new AcessoNegadoException("Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO");
        }
        return usuario;
    }
}
