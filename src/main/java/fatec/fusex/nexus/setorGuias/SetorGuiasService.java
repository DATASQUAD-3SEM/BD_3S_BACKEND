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
import fatec.fusex.nexus.common.auth.UsuarioLogado;
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
import org.springframework.http.MediaType;
import fatec.fusex.nexus.minhasPreGuias.dto.EncaminhamentoArquivo;

/**
 * Regras de leitura do Setor de Guias: fila paginada com busca e filtro de status (T3-BE1),
 * detalhe da pre-guia (T4-BE1) e arquivo do encaminhamento (T4-BE2). O caminho do arquivo nunca e exposto.
 */
@Service
public class SetorGuiasService {

    private static final int SIZE_PADRAO = 20;
    private static final int SIZE_MAXIMO = 100;

    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;
    private final UsuarioLogado usuarioLogado;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    public SetorGuiasService(PreGuiaRepository preGuiaRepository,
                             PreGuiaProcedimentoRepository preGuiaProcedimentoRepository,
                             GuiaRepository guiaRepository,
                             GuiaRascunhoRepository guiaRascunhoRepository,
                             UsuarioLogado usuarioLogado,
                             ArmazenamentoArquivo armazenamentoArquivo) {
        this.preGuiaRepository = preGuiaRepository;
        this.preGuiaProcedimentoRepository = preGuiaProcedimentoRepository;
        this.guiaRepository = guiaRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
        this.usuarioLogado = usuarioLogado;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @Transactional(readOnly = true)
    public FilaPreGuiaPaginaResponse listar(String busca, String statusRaw, int page, int size) {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

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
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

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

    /** T4-BE2: devolve o arquivo do encaminhamento para visualizacao inline. Nao expoe o caminho. */
    @Transactional(readOnly = true)
    public EncaminhamentoArquivo baixarEncaminhamento(Long idPreGuia) {
        usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS);

        PreGuia preGuia = preGuiaRepository.findById(idPreGuia)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pre-guia nao encontrada: id " + idPreGuia));

        String caminho = preGuia.getEncaminhamentoUrl();
        if (preGuia.getMedicoFusex() != null || caminho == null || caminho.isBlank()) {
            throw new RecursoNaoEncontradoException("Encaminhamento nao encontrado");
        }

        String extensao = caminho.substring(caminho.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        Resource recurso = armazenamentoArquivo.carregar(caminho);
        String filename = "encaminhamento-" + String.format("%04d", preGuia.getIdPreGuia())
                + "." + extensao;

        return new EncaminhamentoArquivo(recurso, MediaType.parseMediaType(tipoDe(caminho)), filename);
    }

    private DetalhePreGuiaResponse.EncaminhamentoDto encaminhamentoDe(String caminho) {
        if (caminho == null) {
            return null;
        }
        String nome = caminho.substring(caminho.lastIndexOf('/') + 1);
        return new DetalhePreGuiaResponse.EncaminhamentoDto(nome, tipoDe(caminho), tamanhoNoArmazenamento(caminho));
    }

    // A leitura do detalhe nao pode falhar se o arquivo sumiu do armazenamento: o tamanho vira 0
    // e o erro aparece apenas no visualizador, ao pedir o arquivo (T4-BE2).
    private long tamanhoNoArmazenamento(String caminho) {
        try {
            return tamanhoDe(armazenamentoArquivo.carregar(caminho));
        } catch (RecursoNaoEncontradoException e) {
            return 0;
        }
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
}