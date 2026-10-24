package fatec.fusex.nexus.setorGuias;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
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
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public SetorGuiasService(PreGuiaRepository preGuiaRepository,
                             UsuarioRepository usuarioRepository,
                             SimulacaoLogin simulacaoLogin) {
        this.preGuiaRepository = preGuiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
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