package fatec.fusex.nexus.guiaAssinatura;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
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
        if (usuario.getAssinaturaUrl() == null || usuario.getAssinaturaUrl().isBlank()) {
            throw new DadosInvalidosException("assinatura",
                    "Cadastre sua assinatura em Minhas informacoes antes de assinar");
        }

        // Trava a Guia antes de ler as assinaturas: a segunda assinatura espera a primeira terminar
        Guia guia = guiaRepository.findByIdComLock(guiaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Guia nao encontrada: id " + guiaId));

        Set<AssinaturaGuiaPapel> jaAssinados = assinaturaGuiaRepository.buscarComLockPorGuia(guiaId)
                .stream()
                .map(AssinaturaGuia::getPapel)
                .collect(Collectors.toSet());

        if (jaAssinados.contains(papel)) {
            throw new ConflitoException(mensagemJaAssinada(papel));
        }

        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setGuia(guia);
        assinatura.setPapel(papel);
        assinatura.setUsuario(usuario);

        try {
            assinaturaGuiaRepository.saveAndFlush(assinatura);
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException(mensagemJaAssinada(papel));
        }

        jaAssinados.add(papel);
        boolean concluida = jaAssinados.containsAll(ORDEM_PAPEIS);
        if (concluida) {
            concluirPreGuia(guia.getPreGuia(), usuario);
        }

        List<AssinaturaGuiaPapel> assinados = ORDEM_PAPEIS.stream()
                .filter(jaAssinados::contains)
                .toList();
        return new AssinaturaRegistradaResponse(assinados, concluida);
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