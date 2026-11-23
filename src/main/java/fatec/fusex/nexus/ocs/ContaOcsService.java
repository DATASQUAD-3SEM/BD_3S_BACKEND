package fatec.fusex.nexus.ocs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Resolve a OCS da conta logada (T19-BE0): a OCS e a de ContaOcs.ocs_id. Conta OCS sem linha em ContaOcs recebe 403.
 */
@Service
public class ContaOcsService {

    private final ContaOcsRepository contaOcsRepository;
    private final UsuarioLogado usuarioLogado;

    public ContaOcsService(ContaOcsRepository contaOcsRepository, UsuarioLogado usuarioLogado) {
        this.contaOcsRepository = contaOcsRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Exige perfil OCS e devolve a OCS vinculada a conta; sem vinculo, 403
    @Transactional(readOnly = true)
    public Ocs ocsDaContaLogada() {
        Usuario usuario = usuarioLogado.exigirPerfil(PerfilUsuario.OCS);
        return contaOcsRepository.findById(usuario.getIdUsuario())
                .map(ContaOcs::getOcs)
                .orElseThrow(() -> new AcessoNegadoException("Conta OCS sem OCS vinculada"));
    }
}
