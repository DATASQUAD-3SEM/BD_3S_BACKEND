package fatec.fusex.nexus.minhasPreGuias;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.common.validation.ValidacaoEncaminhamento;
import fatec.fusex.nexus.minhasPreGuias.dto.ReenvioPreGuiaResponse;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T9-BE4: POST /beneficiarios/me/pre-guias/{id}/reenvio (multipart com ocsId e/ou arquivo).
 * A mesma pre-guia volta para Pendente. O novo arquivo vira o encaminhamento atual e o anterior fica guardado.
 * Regra 11 do modelo: so o beneficiario titular corrige pre-guia criada por ele (medico_id nulo).
 */
@Service
public class ReenvioPreGuiaService {

    private static final String PASTA = "encaminhamentos";
    private static final int OBSERVACAO_MAXIMO = 500; // tamanho da coluna PreGuiaHistorico.observacao

    private final ReenvioPreGuiaRepository preGuiaRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final UsuarioLogado usuarioLogado;
    private final ValidacaoEncaminhamento validacaoEncaminhamento;

    public ReenvioPreGuiaService(ReenvioPreGuiaRepository preGuiaRepository,
                                 OcsRepository ocsRepository,
                                 PreGuiaHistoricoRepository historicoRepository,
                                 ArmazenamentoArquivo armazenamentoArquivo,
                                 UsuarioLogado usuarioLogado,
                                 ValidacaoEncaminhamento validacaoEncaminhamento) {
        this.preGuiaRepository = preGuiaRepository;
        this.ocsRepository = ocsRepository;
        this.historicoRepository = historicoRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.usuarioLogado = usuarioLogado;
        this.validacaoEncaminhamento = validacaoEncaminhamento;
    }

    // Ordem: perfil (403) -> de outro ou inexistente (404) -> origem medico (403) -> status (409) -> corpo (422)
    @Transactional
    public ReenvioPreGuiaResponse reenviar(Long id, Long ocsId, MultipartFile arquivo) {
        Usuario beneficiario = usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO);

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
            validacaoEncaminhamento.validar(arquivo);
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
                armazenamentoArquivo.removerSemFalhar(arquivoNovo); // nao deixa arquivo orfao se o banco falhar
            }
            throw e;
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
}
