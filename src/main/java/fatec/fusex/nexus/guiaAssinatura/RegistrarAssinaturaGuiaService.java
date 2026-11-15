package fatec.fusex.nexus.guiaAssinatura;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.dto.AssinaturaRegistradaResponse;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Registra a assinatura da Guia (T5-BE3). O papel vem do perfil do usuario logado.
 * A assinatura gravada e um snapshot: aponta para a Assinatura vigente do usuario naquele momento,
 * e nao muda se o usuario cadastrar uma assinatura nova depois.
 * A segunda assinatura do mesmo papel recebe 409. Com as duas assinaturas, a pre-guia passa a Concluida
 * e o historico registra a mudanca.
 */
@Service
public class RegistrarAssinaturaGuiaService {

    private static final List<AssinaturaGuiaPapel> ORDEM_PAPEIS =
            List.of(AssinaturaGuiaPapel.MEDICO, AssinaturaGuiaPapel.CHEFE);

    private final GuiaRepository guiaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final UsuarioLogado usuarioLogado;

    public RegistrarAssinaturaGuiaService(GuiaRepository guiaRepository,
                                          AssinaturaGuiaRepository assinaturaGuiaRepository,
                                          PreGuiaHistoricoRepository historicoRepository,
                                          UsuarioLogado usuarioLogado) {
        this.guiaRepository = guiaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.historicoRepository = historicoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional
    public AssinaturaRegistradaResponse assinar(Long guiaId) {
        Usuario usuario = usuarioLogado.atual();
        AssinaturaGuiaPapel papel = papelDoPerfil(usuario);

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Usuario inativo: nao pode assinar Guias");
        }

        Assinatura assinaturaVigente = usuario.getAssinatura();
        if (assinaturaVigente == null) {
            throw new DadosInvalidosException("assinatura",
                    "Cadastre sua assinatura em Minhas informacoes antes de assinar");
        }

        Guia guia = guiaRepository.findByIdComLock(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + guiaId));

        List<AssinaturaGuia> assinaturasDaGuia = assinaturaGuiaRepository.buscarComLockPorGuia(guiaId);
        exigirPapelLivre(papel, usuario, assinaturasDaGuia);

        if (guia.getPreGuia().getStatus() == PreGuiaStatus.CONCLUIDA) {
            throw new AssinaturaGuiaConflitoException(MotivoFalhaAssinatura.CONCLUIDA,
                    "Esta Guia ja esta concluida");
        }

        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setGuia(guia);
        assinatura.setPapel(papel);
        assinatura.setUsuario(usuario);
        assinatura.setAssinatura(assinaturaVigente);

        try {
            assinaturaGuiaRepository.saveAndFlush(assinatura);
        } catch (DataIntegrityViolationException e) {
            throw new AssinaturaGuiaConflitoException(motivoJaAssinado(papel, usuario, null),
                    mensagemJaAssinada(papel));
        }

        Set<AssinaturaGuiaPapel> assinados = assinaturasDaGuia.stream()
                .map(AssinaturaGuia::getPapel)
                .collect(Collectors.toSet());
        assinados.add(papel);
        boolean concluida = assinados.containsAll(ORDEM_PAPEIS);
        if (concluida) {
            concluirPreGuia(guia.getPreGuia(), usuario);
        }

        List<AssinaturaGuiaPapel> papeisAssinados = ORDEM_PAPEIS.stream()
                .filter(assinados::contains)
                .toList();
        return new AssinaturaRegistradaResponse(papeisAssinados, concluida);
    }

    private static void exigirPapelLivre(AssinaturaGuiaPapel papel, Usuario usuario,
                                         List<AssinaturaGuia> assinaturasDaGuia) {
        Optional<AssinaturaGuia> ocupada = assinaturasDaGuia.stream()
                .filter(assinatura -> assinatura.getPapel() == papel)
                .findFirst();
        if (ocupada.isPresent()) {
            throw new AssinaturaGuiaConflitoException(motivoJaAssinado(papel, usuario, ocupada.get()),
                    mensagemJaAssinada(papel));
        }
    }

    private static MotivoFalhaAssinatura motivoJaAssinado(AssinaturaGuiaPapel papel, Usuario usuario,
                                                          AssinaturaGuia assinatura) {
        if (papel == AssinaturaGuiaPapel.CHEFE) {
            return MotivoFalhaAssinatura.JA_ASSINADA_PELO_CHEFE;
        }
        return assinadaPeloUsuario(assinatura, usuario)
                ? MotivoFalhaAssinatura.JA_ASSINADA_PELO_USUARIO
                : MotivoFalhaAssinatura.JA_ASSINADA_POR_MEDICO;
    }

    private static boolean assinadaPeloUsuario(AssinaturaGuia assinatura, Usuario usuario) {
        return assinatura != null
                && assinatura.getUsuario() != null
                && usuario.getIdUsuario() != null
                && usuario.getIdUsuario().equals(assinatura.getUsuario().getIdUsuario());
    }

    private void concluirPreGuia(PreGuia preGuia, Usuario usuario) {
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        historicoRepository.save(new PreGuiaHistorico(
                null,
                preGuia,
                PreGuiaStatus.CONCLUIDA,
                null,
                preGuia.getEncaminhamentoUrl(),
                usuario,
                LocalDateTime.now()));
    }

    private static String mensagemJaAssinada(AssinaturaGuiaPapel papel) {
        return papel == AssinaturaGuiaPapel.MEDICO
                ? "Esta Guia ja foi assinada por um medico do FUSEX"
                : "Esta Guia ja foi assinada pelo chefe do FUSEx";
    }

    private static AssinaturaGuiaPapel papelDoPerfil(Usuario usuario) {
        return switch (usuario.getPerfil()) {
            case MEDICO -> AssinaturaGuiaPapel.MEDICO;
            case CHEFE_FUSEX -> AssinaturaGuiaPapel.CHEFE;
            default -> throw new AcessoNegadoException(
                    "Acesso restrito a medicos e chefe do FUSEX");
        };
    }
}