package fatec.fusex.nexus.setorGuias;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimento;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.setorGuias.dto.DetalhePreGuiaResponse;
import fatec.fusex.nexus.setorGuias.dto.FilaPreGuiaItemResponse;
import fatec.fusex.nexus.setorGuias.dto.FilaPreGuiaPaginaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class SetorGuiasService {

    private static final int SIZE_PADRAO = 20;
    private static final int SIZE_MAXIMO = 100;

    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    public SetorGuiasService(PreGuiaRepository preGuiaRepository,
                             PreGuiaProcedimentoRepository preGuiaProcedimentoRepository,
                             GuiaRepository guiaRepository,
                             GuiaRascunhoRepository guiaRascunhoRepository,
                             UsuarioRepository usuarioRepository,
                             SimulacaoLogin simulacaoLogin,
                             ArmazenamentoArquivo armazenamentoArquivo) {
        this.preGuiaRepository = preGuiaRepository;
        this.preGuiaProcedimentoRepository = preGuiaProcedimentoRepository;
        this.guiaRepository = guiaRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @Transactional(readOnly = true)
    public FilaPreGuiaPaginaResponse listar(String busca, String statusRaw, int page, int size) {
        validarSetorGuias();

        if (page < 0) {
            throw new RegraDeNegocioException("page deve ser >= 0");
        }
        if (size < 1 || size > SIZE_MAXIMO) {
            throw new RegraDeNegocioException("size deve estar entre 1 e " + SIZE_MAXIMO);
        }

        PreGuiaStatus status = StatusFila.parse(statusRaw);
        String buscaNormalizada = StatusFila.normalizar(busca == null ? "" : busca);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dataCriacao"));

        Page<PreGuia> pagina = preGuiaRepository.buscarFila(status, buscaNormalizada, pageable);

        List<FilaPreGuiaItemResponse> itens = pagina.getContent().stream()
                .map(FilaPreGuiaItemResponse::de)
                .toList();

        return new FilaPreGuiaPaginaResponse(
                pagina.getTotalElements(),
                pagina.getNumber(),
                pagina.getSize(),
                itens);
    }

    /** T4-BE1: leitura pura. Nao altera nada. */
    @Transactional(readOnly = true)
    public DetalhePreGuiaResponse detalhar(Long idPreGuia) {
        validarSetorGuias();

        PreGuia preGuia = preGuiaRepository.findById(idPreGuia)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pre-guia nao encontrada: id " + idPreGuia));

        Beneficiario beneficiario = preGuia.getBeneficiario();
        boolean origemMedico = preGuia.getMedicoFusex() != null;

        DetalhePreGuiaResponse.BeneficiarioDto beneficiarioDto =
                new DetalhePreGuiaResponse.BeneficiarioDto(
                        beneficiario.getNome(),
                        beneficiario.getPrecCp(),
                        FaixaEtaria.de(beneficiario.idadeEm(Beneficiario.hoje())).getRotulo());

        List<DetalhePreGuiaResponse.ProcedimentoDto> procedimentos =
                preGuiaProcedimentoRepository.buscarPorPreGuia(idPreGuia).stream()
                        .map(PreGuiaProcedimento::getProcedimentoExame)
                        .map(p -> new DetalhePreGuiaResponse.ProcedimentoDto(
                                p.getCodigoTuss(),
                                p.getTerminologiaProcedimento()))
                        .toList();

        DetalhePreGuiaResponse.MedicoCriadorDto medicoCriador = null;
        if (origemMedico) {
            medicoCriador = new DetalhePreGuiaResponse.MedicoCriadorDto(
                    preGuia.getMedicoFusex().getPatente(),
                    preGuia.getMedicoFusex().getNome(),
                    preGuia.getMedicoFusex().getCrm());
        }

        DetalhePreGuiaResponse.EncaminhamentoDto encaminhamento =
                origemMedico ? null : encaminhamentoDe(preGuia.getEncaminhamentoUrl());

        DetalhePreGuiaResponse.GuiaRascunhoDto rascunho = guiaRascunhoRepository.findById(idPreGuia)
                .map(r -> new DetalhePreGuiaResponse.GuiaRascunhoDto(r.getNumero(), r.getNomeArquivo()))
                .orElse(null);

        boolean guiaConcluida = guiaRepository.findByPreGuiaIdPreGuia(idPreGuia).isPresent();

        return new DetalhePreGuiaResponse(
                preGuia.getIdPreGuia(),
                String.format("%04d", preGuia.getIdPreGuia()),
                preGuia.getStatus().name(),
                origemMedico ? "MEDICO" : "BENEFICIARIO",
                beneficiarioDto,
                preGuia.getOcs().getNome(),
                procedimentos,
                medicoCriador,
                encaminhamento,
                rascunho,
                guiaConcluida);
    }

    private DetalhePreGuiaResponse.EncaminhamentoDto encaminhamentoDe(String caminho) {
        if (caminho == null) {
            return null;
        }
        Resource arquivo = armazenamentoArquivo.carregar(caminho);
        String nome = caminho.substring(caminho.lastIndexOf('/') + 1);
        return new DetalhePreGuiaResponse.EncaminhamentoDto(nome, tipoDe(caminho), tamanhoDe(arquivo));
    }

    private static String tipoDe(String caminho) {
        String ext = caminho.substring(caminho.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }

    private static long tamanhoDe(Resource arquivo) {
        try {
            return arquivo.contentLength();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o tamanho do arquivo", e);
        }
    }

    private void validarSetorGuias() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.SETOR_GUIAS) {
            throw new AcessoNegadoException(
                    "Acesso negado: este endpoint e exclusivo do SETOR_GUIAS");
        }
    }
}