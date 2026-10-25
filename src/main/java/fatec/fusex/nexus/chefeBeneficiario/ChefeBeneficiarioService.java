package fatec.fusex.nexus.chefeBeneficiario;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioItemResponse;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioListaResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class ChefeBeneficiarioService {

    private final BeneficiarioRepository beneficiarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public ChefeBeneficiarioService(BeneficiarioRepository beneficiarioRepository,
                                    UsuarioRepository usuarioRepository,
                                    SimulacaoLogin simulacaoLogin) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @Transactional(readOnly = true)
    public BeneficiarioListaResponse listar(String busca, String situacao, int page, int size) {
        validarChefe();

        Boolean ativo = null;
        if ("ativos".equalsIgnoreCase(situacao)) ativo = true;
        else if ("inativos".equalsIgnoreCase(situacao)) ativo = false;
        else if (situacao != null && !situacao.isBlank() && !"todos".equalsIgnoreCase(situacao)) {
            throw new RegraDeNegocioException("situacao inválida. Use ativos, inativos ou todos.");
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("nome").ascending());

        String buscaNormalizada = "";
        if (busca != null && !busca.isBlank()) {
            buscaNormalizada = Normalizer.normalize(busca.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT);
        }

        Page<Beneficiario> pageResult = beneficiarioRepository.buscarComFiltros(buscaNormalizada, ativo, pageable);
        return BeneficiarioListaResponse.de(pageResult.map(BeneficiarioItemResponse::de));
    }

    private void validarChefe() {
        String login = simulacaoLogin.loginAtual();
        Usuario u = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuário logado não encontrado"));
        if (u.getPerfil() != PerfilUsuario.CHEFE_FUSEX) {
            throw new AcessoNegadoException("Acesso restrito ao Chefe do FUSEX");
        }
    }
}