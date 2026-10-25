package fatec.fusex.nexus.guiaAssinatura;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.dto.GuiaParaAssinarResponse;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Lista as Guias que ainda precisam da assinatura do papel do usuario logado (T5-BE1),
 * da mais antiga para a mais recente, indicando o que falta assinar em cada uma.
 */
@Service
public class GuiaAssinaturaService {

    private static final List<AssinaturaGuiaPapel> ORDEM_PAPEIS =
            List.of(AssinaturaGuiaPapel.MEDICO, AssinaturaGuiaPapel.CHEFE);

    private final GuiaRepository guiaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;
    private final UsuarioLogado usuarioLogado;

    public GuiaAssinaturaService(GuiaRepository guiaRepository,
                                 AssinaturaGuiaRepository assinaturaGuiaRepository,
                                 UsuarioLogado usuarioLogado) {
        this.guiaRepository = guiaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public List<GuiaParaAssinarResponse> listarParaAssinar() {
        AssinaturaGuiaPapel papel = papelDoPerfil(usuarioLogado.atual());

        List<Guia> guias = guiaRepository.buscarSemAssinatura(papel);
        if (guias.isEmpty()) {
            return List.of();
        }

        Map<Long, Set<AssinaturaGuiaPapel>> assinadas = papeisAssinadosPorGuia(guias);

        return guias.stream()
                .map(guia -> GuiaParaAssinarResponse.de(
                        guia,
                        pendencias(assinadas.getOrDefault(guia.getIdGuia(), Set.of()))))
                .toList();
    }

    private Map<Long, Set<AssinaturaGuiaPapel>> papeisAssinadosPorGuia(List<Guia> guias) {
        List<Long> ids = guias.stream().map(Guia::getIdGuia).toList();

        return assinaturaGuiaRepository.findByGuiaIdGuiaIn(ids).stream()
                .collect(Collectors.groupingBy(
                        a -> a.getGuia().getIdGuia(),
                        Collectors.mapping(AssinaturaGuia::getPapel, Collectors.toSet())));
    }

    private static List<AssinaturaGuiaPapel> pendencias(Set<AssinaturaGuiaPapel> assinados) {
        return ORDEM_PAPEIS.stream()
                .filter(papel -> !assinados.contains(papel))
                .toList();
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