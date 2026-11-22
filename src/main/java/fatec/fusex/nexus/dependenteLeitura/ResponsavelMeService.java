package fatec.fusex.nexus.dependenteLeitura;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.dependente.ResponsavelExterno;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * T31-BE5: dados pessoais do responsavel legal externo para Minhas informacoes (so leitura): nome, CPF (o login)
 * e se ja cadastrou a assinatura com que assina as sessoes do menor. Nao tem Prec CP.
 */
@Service
public class ResponsavelMeService {

    private final UsuarioLogado usuarioLogado;
    private final ResponsavelExternoRepository responsavelExternoRepository;

    public ResponsavelMeService(UsuarioLogado usuarioLogado, ResponsavelExternoRepository responsavelExternoRepository) {
        this.usuarioLogado = usuarioLogado;
        this.responsavelExternoRepository = responsavelExternoRepository;
    }

    @Transactional(readOnly = true)
    public ResponsavelMeResponse meusDados() {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.RESPONSAVEL_EXTERNO);
        String nome = responsavelExternoRepository.findById(usuario.getIdUsuario())
                .map(ResponsavelExterno::getNome)
                .orElse(null);
        return new ResponsavelMeResponse(nome, usuario.getLogin(), usuario.getAssinatura() != null);
    }

    public record ResponsavelMeResponse(String nome, String cpf, boolean temAssinatura) {
    }
}
