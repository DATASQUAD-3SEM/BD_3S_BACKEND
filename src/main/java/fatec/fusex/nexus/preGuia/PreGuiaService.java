package fatec.fusex.nexus.preGuia;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsService;
import fatec.fusex.nexus.preGuia.dto.CorrecaoPreGuiaResponse;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;


@Service
public class PreGuiaService {

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsService ocsService;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final UsuarioRepository usuarioRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    public PreGuiaService(PreGuiaRepository preGuiaRepository,
                          BeneficiarioRepository beneficiarioRepository,
                          OcsService ocsService,
                          ArmazenamentoArquivo armazenamentoArquivo,
                          UsuarioRepository usuarioRepository,
                          PreGuiaHistoricoRepository historicoRepository) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsService = ocsService;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.usuarioRepository = usuarioRepository;
        this.historicoRepository = historicoRepository;
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

    @Transactional
    public PreGuiaResponse criar(String cpf, String precCp, Long ocsId, MultipartFile arquivo) {
        validarArquivoObrigatorio(arquivo);

        Ocs ocs = ocsService.buscarPorId(ocsId);
        Beneficiario beneficiario = identificarBeneficiario(cpf, precCp);

        String encaminhamentoUrl = armazenamentoArquivo.salvar(arquivo, "encaminhamentos");
        try {
            PreGuia preGuia = new PreGuia();
            preGuia.setBeneficiario(beneficiario);
            preGuia.setOcs(ocs);
            preGuia.setEncaminhamentoUrl(encaminhamentoUrl);

            preGuiaRepository.save(preGuia);

            return PreGuiaResponse.de(preGuia);
        } catch (RuntimeException e) {
            try {
                armazenamentoArquivo.remover(encaminhamentoUrl);
            } catch (RuntimeException limpezaFalhou) {
                // nao deixa a falha da limpeza esconder a causa original
            }
            throw e;
        }
    }

    private Beneficiario identificarBeneficiario(String cpf, String precCp) {
        Beneficiario beneficiario = beneficiarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado: cpf " + cpf));

        if (!beneficiario.getPrecCp().equals(precCp)) {
            throw new RegraDeNegocioException("CPF e Prec-CP nao correspondem ao mesmo beneficiario");
        }
        return beneficiario;
    }

    private void validarArquivoObrigatorio(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("E necessario anexar o encaminhamento medico para criar a pre-guia");
        }
    }

    // T9-BE1: dados da tela "Pre-guia {numero}" em correcao.
    // Ordem das validacoes: perfil (403) -> dono (404) -> origem medico (403) -> status Atencao (409).
    @Transactional(readOnly = true)
    public CorrecaoPreGuiaResponse buscarDadosCorrecao(Long preGuiaId, String login) {
        Usuario usuario = validarPerfilBeneficiario(login);

        // Pre-guia de outro beneficiario devolve 404, para nao revelar que ela existe
        PreGuia preGuia = preGuiaRepository.findById(preGuiaId)
                .filter(pg -> pg.getBeneficiario().getUsuarioId().equals(usuario.getIdUsuario()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada: id " + preGuiaId));

        // Mesma regra do MinhaPreGuiaResponse: com medico, a origem e MEDICO
        if (preGuia.getMedicoFusex() != null) {
            throw new AcessoNegadoException("Pre-guia criada por medico nao pode ser alterada pelo beneficiario");
        }

        if (preGuia.getStatus() != PreGuiaStatus.ATENCAO) {
            throw new ConflitoException("A pre-guia nao esta em Atencao");
        }

        Beneficiario beneficiario = preGuia.getBeneficiario();
        Ocs ocs = preGuia.getOcs();

        return new CorrecaoPreGuiaResponse(
                String.format("%04d", preGuia.getIdPreGuia()),
                preGuia.getStatus().name(),
                new CorrecaoPreGuiaResponse.BeneficiarioResumo(beneficiario.getNome(), beneficiario.getPrecCp()),
                preGuia.getDataCriacao(),
                observacaoDoSetor(preGuia),
                encaminhamentoAtual(preGuia),
                new CorrecaoPreGuiaResponse.OcsResumo(ocs.getIdOcs(), ocs.getNome()));
    }

    private Usuario validarPerfilBeneficiario(String login) {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.BENEFICIARIO) {
            throw new AcessoNegadoException("Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO");
        }
        return usuario;
    }

    // A observacao fica no historico: e a ultima linha em ATENCAO
    private CorrecaoPreGuiaResponse.ObservacaoResumo observacaoDoSetor(PreGuia preGuia) {
        return historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                .map(h -> new CorrecaoPreGuiaResponse.ObservacaoResumo(h.getObservacao(), h.getDataHora()))
                .orElse(null);
    }

    private CorrecaoPreGuiaResponse.EncaminhamentoResumo encaminhamentoAtual(PreGuia preGuia) {
        String url = preGuia.getEncaminhamentoUrl();
        if (url == null) {
            return null;
        }
        String nome = url.substring(url.lastIndexOf('/') + 1);
        String tipo = MediaTypeFactory.getMediaType(nome).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();

        // Sem registro no historico, o arquivo e o enviado na criacao da pre-guia
        LocalDateTime dataEnvio = historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndEncaminhamentoUrlOrderByDataHoraDesc(preGuia.getIdPreGuia(), url)
                .map(PreGuiaHistorico::getDataHora)
                .orElse(preGuia.getDataCriacao());

        return new CorrecaoPreGuiaResponse.EncaminhamentoResumo(nome, tipo, dataEnvio);
    }
}