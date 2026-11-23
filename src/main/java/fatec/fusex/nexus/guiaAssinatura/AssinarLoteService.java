package fatec.fusex.nexus.guiaAssinatura;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.guiaAssinatura.dto.AssinarLoteResponse;
import fatec.fusex.nexus.guiaAssinatura.dto.FalhaAssinaturaLote;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@Service
public class AssinarLoteService {

    static final int LIMITE_GUIAS_POR_LOTE = 50;

    private final RegistrarAssinaturaGuiaService registrarAssinaturaGuiaService;
    private final LimiteLoteAssinatura limiteLoteAssinatura;
    private final UsuarioLogado usuarioLogado;

    public AssinarLoteService(RegistrarAssinaturaGuiaService registrarAssinaturaGuiaService,
                              LimiteLoteAssinatura limiteLoteAssinatura,
                              UsuarioLogado usuarioLogado) {
        this.registrarAssinaturaGuiaService = registrarAssinaturaGuiaService;
        this.limiteLoteAssinatura = limiteLoteAssinatura;
        this.usuarioLogado = usuarioLogado;
    }

    // Cada guia tem a propria transacao (RegistrarAssinaturaGuiaService), entao uma falha nao desfaz as outras
    public AssinarLoteResponse assinar(List<Long> guiaIds) {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.MEDICO, PerfilUsuario.CHEFE_FUSEX);

        if (usuario.getAssinatura() == null) {
            throw new DadosInvalidosException("assinatura",
                    "Cadastre sua assinatura em Minhas informacoes antes de assinar");
        }

        List<Long> ids = validarEOrdenar(guiaIds);
        limiteLoteAssinatura.registrar(usuario.getLogin());

        List<Long> assinadas = new ArrayList<>();
        List<FalhaAssinaturaLote> falhas = new ArrayList<>();

        for (Long guiaId : ids) {
            try {
                registrarAssinaturaGuiaService.assinar(guiaId);
                assinadas.add(guiaId);
            } catch (RecursoNaoEncontradoException e) {
                falhas.add(new FalhaAssinaturaLote(guiaId, MotivoFalhaAssinatura.GUIA_NAO_ENCONTRADA.getCodigo()));
            } catch (AssinaturaGuiaConflitoException e) {
                falhas.add(new FalhaAssinaturaLote(guiaId, e.getMotivo().getCodigo()));
            }
        }

        return new AssinarLoteResponse(assinadas, falhas);
    }

    // Remove repetidos, ordena e rejeita lista vazia, id invalido ou mais de 50 guias (400)
    static List<Long> validarEOrdenar(List<Long> guiaIds) {
        if (guiaIds == null || guiaIds.isEmpty()) {
            throw new RegraDeNegocioException("Selecione ao menos uma Guia para assinar");
        }
        if (guiaIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new RegraDeNegocioException("A lista contem um identificador de Guia invalido");
        }

        Set<Long> unicos = new TreeSet<>(guiaIds);
        if (unicos.size() > LIMITE_GUIAS_POR_LOTE) {
            throw new RegraDeNegocioException("Selecione ate " + LIMITE_GUIAS_POR_LOTE + " guias por vez.");
        }
        return new ArrayList<>(unicos);
    }
}