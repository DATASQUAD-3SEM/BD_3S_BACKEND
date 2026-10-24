package fatec.fusex.nexus.MinhasPreGuias;

import fatec.fusex.nexus.MinhasPreGuias.dto.CorrecaoPreGuiaResponse;
import fatec.fusex.nexus.MinhasPreGuias.dto.GuiaPdf;
import fatec.fusex.nexus.MinhasPreGuias.dto.MinhaPreGuiaResponse;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class MinhasPreGuiasService {

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final GuiaRepository guiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    public MinhasPreGuiasService(PreGuiaRepository preGuiaRepository,
                                 BeneficiarioRepository beneficiarioRepository,
                                 PreGuiaHistoricoRepository historicoRepository,
                                 GuiaRepository guiaRepository,
                                 UsuarioRepository usuarioRepository,
                                 SimulacaoLogin simulacaoLogin,
                                 AssinaturaGuiaRepository assinaturaGuiaRepository,
                                 ArmazenamentoArquivo armazenamentoArquivo) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.historicoRepository = historicoRepository;
        this.guiaRepository = guiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @Transactional(readOnly = true)
    public List<MinhaPreGuiaResponse> listar() {
        // ... igual ao T1-BE1 (mantém)
        Beneficiario beneficiario = beneficiarioLogado();

        List<PreGuia> preGuias = preGuiaRepository
                .findByBeneficiarioUsuarioIdOrderByDataCriacaoDesc(beneficiario.getUsuarioId());

        return preGuias.stream().map(preGuia -> {

            String obs = null;
            if (preGuia.getStatus() == PreGuiaStatus.ATENCAO) {
                obs = historicoRepository
                        .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(
                                preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                        .map(PreGuiaHistorico::getObservacao)
                        .orElse(null);
            }

            MinhaPreGuiaResponse.GuiaDto guiaDto = null;
            if (preGuia.getStatus() == PreGuiaStatus.CONCLUIDA) {
                guiaDto = guiaRepository.findByPreGuiaIdPreGuia(preGuia.getIdPreGuia())
                        .map(guia -> new MinhaPreGuiaResponse.GuiaDto(
                                guia.getNumero(),
                                guia.getDataInsercao().toLocalDate().plusMonths(2)
                        ))
                        .orElse(null);
            }

            return MinhaPreGuiaResponse.fromEntity(preGuia, obs, guiaDto);
        }).toList();
    }

    @Transactional(readOnly = true)
    public CorrecaoPreGuiaResponse buscarCorrecao(Long idPreGuia) {
        Beneficiario beneficiario = beneficiarioLogado();

        PreGuia preGuia = preGuiaRepository.findById(idPreGuia)
                .filter(p -> p.getBeneficiario().getUsuarioId().equals(beneficiario.getUsuarioId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pre-guia nao encontrada"));

        if (preGuia.getMedicoFusex() != null) {
            throw new AcessoNegadoException(
                    "Pre-guia criada por medico nao pode ser corrigida pelo beneficiario");
        }

        if (preGuia.getStatus() != PreGuiaStatus.ATENCAO) {
            throw new ConflitoException(
                    "Pre-guia nao esta em Atencao: status atual " + preGuia.getStatus());
        }

        CorrecaoPreGuiaResponse.ObservacaoDto observacao = historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(idPreGuia, PreGuiaStatus.ATENCAO)
                .map(h -> new CorrecaoPreGuiaResponse.ObservacaoDto(h.getObservacao(), h.getDataHora()))
                .orElse(null);

        Beneficiario dono = preGuia.getBeneficiario();

        return new CorrecaoPreGuiaResponse(
                String.format("%04d", preGuia.getIdPreGuia()),
                preGuia.getStatus(),
                new CorrecaoPreGuiaResponse.BeneficiarioDto(dono.getNome(), dono.getPrecCp()),
                preGuia.getDataCriacao(),
                observacao,
                encaminhamentoAtual(preGuia),
                new CorrecaoPreGuiaResponse.OcsDto(
                        preGuia.getOcs().getIdOcs(),
                        preGuia.getOcs().getNome())
        );
    }

    @Transactional(readOnly = true)
    public GuiaPdf baixarGuiaPdf(Long idPreGuia, String modoRaw) {
        ModoDownload modo = ModoDownload.from(modoRaw);
        Beneficiario beneficiario = beneficiarioLogado();

        PreGuia preGuia = preGuiaRepository.findById(idPreGuia)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pre-guia nao encontrada"));

        if (!preGuia.getBeneficiario().getUsuarioId().equals(beneficiario.getUsuarioId())) {
            throw new RecursoNaoEncontradoException("Pre-guia nao encontrada");
        }

        if (preGuia.getStatus() != PreGuiaStatus.CONCLUIDA) {
            throw new AcessoNegadoException(
                    "Pre-guia ainda nao esta concluida: PDF da guia indisponivel");
        }

        long assinaturas = assinaturaGuiaRepository.countByGuiaPreGuiaIdPreGuia(idPreGuia);
        if (assinaturas < 2) {
            throw new AcessoNegadoException(
                    "Guia sem as 2 assinaturas necessarias: PDF indisponivel");
        }

        Guia guia = guiaRepository.findByPreGuiaIdPreGuia(idPreGuia)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Guia nao encontrada"));

        String caminho = guia.getArquivoUrl();
        if (caminho == null || !caminho.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new RegraDeNegocioException("Arquivo da guia nao e um PDF");
        }

        Resource resource = armazenamentoArquivo.carregar(caminho);
        String filename = "guia-" + sanitizar(guia.getNumero()) + ".pdf";

        return new GuiaPdf(resource, filename, modo == ModoDownload.VISUALIZAR);
    }

    private CorrecaoPreGuiaResponse.EncaminhamentoDto encaminhamentoAtual(PreGuia preGuia) {
        String url = preGuia.getEncaminhamentoUrl();
        if (url == null || url.isBlank()) {
            return null;
        }

        // Observacoes do setor (status ATENCAO) tambem guardam a URL atual; por isso
        // a data de envio vem do ultimo registro que nao seja de observacao (ex.: reenvio).
        LocalDateTime dataEnvio = historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndEncaminhamentoUrlAndStatusNotOrderByDataHoraDesc(
                        preGuia.getIdPreGuia(), url, PreGuiaStatus.ATENCAO)
                .map(PreGuiaHistorico::getDataHora)
                .orElse(preGuia.getDataCriacao());

        return new CorrecaoPreGuiaResponse.EncaminhamentoDto(
                nomeDoArquivo(url),
                tipoDoArquivo(url),
                dataEnvio);
    }

    private static String nomeDoArquivo(String caminho) {
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    private static String tipoDoArquivo(String caminho) {
        int ponto = caminho.lastIndexOf('.');
        return ponto < 0 ? null : caminho.substring(ponto + 1).toUpperCase(Locale.ROOT);
    }

    private Beneficiario beneficiarioLogado() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.BENEFICIARIO) {
            throw new AcessoNegadoException(
                    "Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO");
        }

        return beneficiarioRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Beneficiario nao encontrado"));
    }

    private static String sanitizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "sem-numero";
        }
        return valor.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}