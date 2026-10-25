package fatec.fusex.nexus.preGuia;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.common.validation.ValidacaoEncaminhamento;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;


/**
 * Criacao da pre-guia pelo beneficiario: grava o encaminhamento enviado e a pre-guia como Pendente.
 *
 * O beneficiario e sempre o usuario logado (nunca um CPF recebido na requisicao), conforme a regra de que
 * os dados pessoais saem do proprio perfil. Se a gravacao no banco falhar, o arquivo salvo e removido
 * para nao ficar orfao.
 */
@Service
public class PreGuiaService {

    private static final String PASTA = "encaminhamentos";

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final UsuarioLogado usuarioLogado;
    private final ValidacaoEncaminhamento validacaoEncaminhamento;

    public PreGuiaService(PreGuiaRepository preGuiaRepository,
                          BeneficiarioRepository beneficiarioRepository,
                          OcsRepository ocsRepository,
                          PreGuiaHistoricoRepository historicoRepository,
                          ArmazenamentoArquivo armazenamentoArquivo,
                          UsuarioLogado usuarioLogado,
                          ValidacaoEncaminhamento validacaoEncaminhamento) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.historicoRepository = historicoRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.usuarioLogado = usuarioLogado;
        this.validacaoEncaminhamento = validacaoEncaminhamento;
    }

    public List<PreGuia> listarDoBeneficiario(Long beneficiarioId) {
        if (!beneficiarioRepository.existsById(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Beneficiario nao encontrado: id " + beneficiarioId);
        }
        return preGuiaRepository.findByBeneficiarioUsuarioId(beneficiarioId);
    }

    public PreGuia buscarDoBeneficiario(Long preGuiaId, Long beneficiarioId) {
        PreGuia preGuia = preGuiaRepository.findById(preGuiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId));

        if (!preGuia.getBeneficiario().getUsuarioId().equals(beneficiarioId)) {
            throw new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId);
        }
        return preGuia;
    }

    // Ordem: perfil (403) -> cadastro do beneficiario (404) -> OCS (422) -> arquivo (422)
    @Transactional
    public PreGuiaResponse criar(Long ocsId, MultipartFile arquivo) {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = beneficiarioRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));

        Ocs ocs = buscarOcsAtiva(ocsId);
        validacaoEncaminhamento.validarObrigatorio(arquivo);

        String encaminhamentoUrl = armazenamentoArquivo.salvar(arquivo, PASTA);
        try {
            PreGuia preGuia = new PreGuia();
            preGuia.setBeneficiario(beneficiario);
            preGuia.setOcs(ocs);
            preGuia.setEncaminhamentoUrl(encaminhamentoUrl);
            preGuia.setStatus(PreGuiaStatus.PENDENTE);
            preGuiaRepository.save(preGuia);

            // Primeira linha do historico: quem criou foi o proprio beneficiario, com a foto enviada
            historicoRepository.save(new PreGuiaHistorico(
                    null, preGuia, PreGuiaStatus.PENDENTE, null, encaminhamentoUrl, usuario, preGuia.getDataCriacao()));

            return PreGuiaResponse.de(preGuia);
        } catch (RuntimeException e) {
            armazenamentoArquivo.removerSemFalhar(encaminhamentoUrl); // nao deixa arquivo orfao se o banco falhar
            throw e;
        }
    }

    private Ocs buscarOcsAtiva(Long ocsId) {
        if (ocsId == null) {
            throw new DadosInvalidosException("ocsId", "Escolha a OCS");
        }
        return ocsRepository.findById(ocsId)
                .filter(ocs -> Boolean.TRUE.equals(ocs.getAtivo()))
                .orElseThrow(() -> new DadosInvalidosException("ocsId", "OCS inexistente ou inativa"));
    }
}
