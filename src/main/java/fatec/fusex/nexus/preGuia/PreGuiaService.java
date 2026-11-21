package fatec.fusex.nexus.preGuia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.common.validation.ValidacaoEncaminhamento;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.dto.PacienteResponse;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Criacao da pre-guia pelo beneficiario ou pelo responsavel externo: grava o encaminhamento enviado e a
 * pre-guia como Pendente. Com dependenteId, o paciente e o dependente (T30-BE2); sem ele, o proprio titular.
 * Se a gravacao no banco falhar, o arquivo salvo e removido para nao ficar orfao.
 */
@Service
public class PreGuiaService {

    private static final String PASTA = "encaminhamentos";
    private static final int IDADE_MENOR = 18;

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;
    private final UsuarioLogado usuarioLogado;
    private final ValidacaoEncaminhamento validacaoEncaminhamento;
    private final DependenteRepository dependenteRepository;

    public PreGuiaService(PreGuiaRepository preGuiaRepository,
                          BeneficiarioRepository beneficiarioRepository,
                          OcsRepository ocsRepository,
                          PreGuiaHistoricoRepository historicoRepository,
                          ArmazenamentoArquivo armazenamentoArquivo,
                          UsuarioLogado usuarioLogado,
                          ValidacaoEncaminhamento validacaoEncaminhamento,
                          DependenteRepository dependenteRepository) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.historicoRepository = historicoRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
        this.usuarioLogado = usuarioLogado;
        this.validacaoEncaminhamento = validacaoEncaminhamento;
        this.dependenteRepository = dependenteRepository;
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

    /**
     * T30-BE2: pacientes que o usuario logado pode escolher para a nova pre-guia. Titular: ele mesmo (se
     * tiver cadastro de beneficiario) e os menores sob sua guarda. Responsavel externo: os menores que
     * acompanha. Dependente maior: so ele mesmo. Vigencia vencida e maiores (exceto o proprio) nao aparecem.
     */
    @Transactional(readOnly = true)
    public List<PacienteResponse> listarPacientes() {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO);
        LocalDate hoje = LocalDate.now(Beneficiario.FUSO);
        List<PacienteResponse> pacientes = new ArrayList<>();

        if (usuario.getPerfil() == PerfilUsuario.BENEFICIARIO) {
            beneficiarioRepository.findById(usuario.getIdUsuario())
                    .ifPresent(b -> pacientes.add(new PacienteResponse(null, b.getNome(), "Você (titular)")));
        }
        for (Dependente dependente : pacientesPermitidos(usuario, hoje)) {
            pacientes.add(new PacienteResponse(dependente.getIdDependente(), dependente.getNome(),
                    rotulo(dependente, usuario)));
        }
        return pacientes;
    }

    // Ordem: perfil (403) -> cadastro do beneficiario (404) -> OCS (422) -> arquivo (422)
    @Transactional
    public PreGuiaResponse criar(Long ocsId, MultipartFile arquivo) {
        return criar(ocsId, null, arquivo);
    }

    @Transactional
    public PreGuiaResponse criar(Long ocsId, Long dependenteId, MultipartFile arquivo) {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO, PerfilUsuario.RESPONSAVEL_EXTERNO);

        Beneficiario beneficiario;
        Dependente dependente = null;
        if (dependenteId != null) {
            dependente = buscarPermitido(dependenteId, usuario);
            beneficiario = dependente.getTitular();
        } else {
            if (usuario.getPerfil() == PerfilUsuario.RESPONSAVEL_EXTERNO) {
                throw new DadosInvalidosException("dependenteId", "Escolha o paciente");
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
            preGuia.setDependente(dependente);
            preGuia.setCriadaPor(usuario);
            preGuia.setOcs(ocs);
            preGuia.setEncaminhamentoUrl(encaminhamentoUrl);
            preGuia.setStatus(PreGuiaStatus.PENDENTE);
            preGuiaRepository.save(preGuia);

            // Primeira linha do historico: quem criou (usuario logado), com a foto enviada
            historicoRepository.save(new PreGuiaHistorico(
                    null, preGuia, PreGuiaStatus.PENDENTE, null, encaminhamentoUrl, usuario, preGuia.getDataCriacao()));

            return PreGuiaResponse.de(preGuia);
        } catch (RuntimeException e) {
            armazenamentoArquivo.removerSemFalhar(encaminhamentoUrl); // nao deixa arquivo orfao se o banco falhar
            throw e;
        }
    }

    /** Dependente da lista de pacientes permitidos; fora dela (outro menor, maior alheio, vencido): 403. */
    private Dependente buscarPermitido(Long dependenteId, Usuario usuario) {
        LocalDate hoje = LocalDate.now(Beneficiario.FUSO);
        return pacientesPermitidos(usuario, hoje).stream()
                .filter(d -> d.getIdDependente().equals(dependenteId))
                .findFirst()
                .orElseThrow(() -> new AcessoNegadoException("Paciente nao permitido para este usuario"));
    }

    private List<Dependente> pacientesPermitidos(Usuario usuario, LocalDate hoje) {
        List<Dependente> lista = new ArrayList<>();
        if (usuario.getPerfil() == PerfilUsuario.RESPONSAVEL_EXTERNO) {
            dependenteRepository.findByResponsavelIdUsuarioAndTipo(usuario.getIdUsuario(), TipoDependente.MENOR_RESPONSAVEL)
                    .stream().filter(d -> menorVigente(d, hoje)).forEach(lista::add);
            return lista;
        }
        dependenteRepository.findByTitularUsuarioIdAndTipo(usuario.getIdUsuario(), TipoDependente.MENOR_TITULAR)
                .stream().filter(d -> menorVigente(d, hoje)).forEach(lista::add);
        dependenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .filter(d -> d.getTipo() == TipoDependente.MAIOR && vigente(d, hoje))
                .ifPresent(lista::add);
        return lista;
    }

    private static boolean menorVigente(Dependente dependente, LocalDate hoje) {
        return dependente.getDataNascimento().plusYears(IDADE_MENOR).isAfter(hoje) && vigente(dependente, hoje);
    }

    private static boolean vigente(Dependente dependente, LocalDate hoje) {
        return dependente.getVigenciaFim() == null || !dependente.getVigenciaFim().isBefore(hoje);
    }

    private static String rotulo(Dependente dependente, Usuario usuario) {
        if (dependente.getTipo() == TipoDependente.MAIOR) {
            return "Você (dependente maior)";
        }
        return "Seu dependente (menor sob sua guarda)";
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
