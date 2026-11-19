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
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.PacienteResponse;
import fatec.fusex.nexus.dependente.PacientesPreGuiaService;
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
    private final PacientesPreGuiaService pacientesPreGuiaService;

    public PreGuiaService(PreGuiaRepository preGuiaRepository,
                          BeneficiarioRepository beneficiarioRepository,
                          OcsRepository ocsRepository,
                          PreGuiaHistoricoRepository historicoRepository,
                          ArmazenamentoArquivo armazenamentoArquivo,
                          UsuarioLogado usuarioLogado,
                          ValidacaoEncaminhamento validacaoEncaminhamento,
                          PacientesPreGuiaService pacientesPreGuiaService) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.historicoRepository = historicoRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.usuarioLogado = usuarioLogado;
        this.validacaoEncaminhamento = validacaoEncaminhamento;
        this.pacientesPreGuiaService = pacientesPreGuiaService;
    }

    /** GET /pre-guias/pacientes (T30-BE2): pacientes que o usuario logado pode escolher para a pre-guia. */
    public List<PacienteResponse> listarPacientes() {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO);
        return pacientesPreGuiaService.listar(usuario);
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

    // Ordem: perfil (403) -> paciente (403 ou 422) -> cadastro do beneficiario (404) -> OCS (422) -> arquivo (422)
    // T30-BE2: com dependenteId, o paciente e o dependente (403 se o usuario nao pode escolhe-lo); sem ele, o titular
    @Transactional
    public PreGuiaResponse criar(Long ocsId, Long dependenteId, MultipartFile arquivo) {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO);

        Dependente dependente = null;
        Beneficiario beneficiario;
        if (dependenteId != null) {
            dependente = pacientesPreGuiaService.dependentePermitido(usuario, dependenteId);
            beneficiario = dependente.getTitular();
        } else {
            if (usuario.getPerfil() == PerfilUsuario.RESPONSAVEL_EXTERNO) {
                throw new DadosInvalidosException("dependenteId", "Escolha o paciente da pre-guia");
            }
            beneficiario = beneficiarioRepository.findById(usuario.getIdUsuario())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
        }

        Ocs ocs = buscarOcsAtiva(ocsId);
        validacaoEncaminhamento.validarObrigatorio(arquivo);

        String encaminhamentoUrl = armazenamentoArquivo.salvar(arquivo, PASTA);
        try {
            PreGuia preGuia = new PreGuia();
            preGuia.setBeneficiario(beneficiario);
            preGuia.setOcs(ocs);
            preGuia.setEncaminhamentoUrl(encaminhamentoUrl);
            preGuia.setStatus(PreGuiaStatus.PENDENTE);
            preGuia.setDependente(dependente);
            // a idade vale no dia da criacao: depois de 18 anos, a pre-guia continua com o responsavel
            preGuia.setPacienteMenorNaCriacao(dependente != null && dependente.menorEm(PacientesPreGuiaService.hoje()));
            preGuia.setCriadaPor(usuario);
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
