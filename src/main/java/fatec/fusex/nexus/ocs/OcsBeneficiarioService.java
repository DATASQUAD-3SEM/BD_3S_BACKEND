package fatec.fusex.nexus.ocs;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.ocs.dto.OcsResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class OcsBeneficiarioService {

    static final int LIMITE_RESULTADOS = 30;

    private final OcsRepository ocsRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public OcsBeneficiarioService(OcsRepository ocsRepository,
                                  UsuarioRepository usuarioRepository,
                                  SimulacaoLogin simulacaoLogin) {
        this.ocsRepository = ocsRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @Transactional(readOnly = true)
    public List<OcsResponse> buscarAtivas(String busca) {
        validarBeneficiario();

        String termo = normalizar(busca);

        return ocsRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(ocs -> termo.isEmpty() || corresponde(ocs, termo))
                .limit(LIMITE_RESULTADOS)
                .map(OcsResponse::de)
                .toList();
    }

    private static boolean corresponde(Ocs ocs, String termo) {
        return normalizar(ocs.getNome()).contains(termo)
                || normalizar(ocs.getContratoNum()).contains(termo)
                || normalizar(ocs.getTipo().name()).contains(termo);
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private void validarBeneficiario() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.BENEFICIARIO) {
            throw new AcessoNegadoException(
                    "Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO");
        }
    }
}