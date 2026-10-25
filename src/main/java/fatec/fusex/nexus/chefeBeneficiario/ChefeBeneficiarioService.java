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

    private static final int SIZE_MAXIMO = 100;

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

    // Perfil (403) vem antes da validacao dos parametros (400)
    @Transactional(readOnly = true)
    public BeneficiarioListaResponse listar(String busca, String situacao, int page, int size) {
        validarChefe();

        if (page < 0) {
            throw new RegraDeNegocioException("page deve ser >= 0");
        }
        if (size < 1 || size > SIZE_MAXIMO) {
            throw new RegraDeNegocioException("size deve estar entre 1 e " + SIZE_MAXIMO);
        }

        Boolean ativo = filtroDeSituacao(situacao);
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());

        String termo = busca == null ? "" : busca.trim();
        String buscaNormalizada = "";
        if (!termo.isEmpty()) {
            buscaNormalizada = escaparCuringas(Normalizer.normalize(termo, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT));
        }

        Page<Beneficiario> pageResult = beneficiarioRepository.buscarComFiltros(
                buscaNormalizada, digitosDaBusca(termo), ativo, pageable);
        return BeneficiarioListaResponse.de(pageResult.map(BeneficiarioItemResponse::de));
    }

    // ativos (padrao, inclusive vazio) -> true; inativos -> false; todos -> null (sem filtro)
    private static Boolean filtroDeSituacao(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return true;
        }
        return switch (situacao.trim().toLowerCase(Locale.ROOT)) {
            case "ativos" -> true;
            case "inativos" -> false;
            case "todos" -> null;
            default -> throw new RegraDeNegocioException("situacao inválida. Use ativos, inativos ou todos.");
        };
    }

    // Busca com cara de CPF/Prec CP (so digitos, pontos, hifen e espacos): compara so os digitos,
    // assim "458722133-00" encontra o Prec CP gravado como 45872213300.
    private static String digitosDaBusca(String termo) {
        if (termo.isEmpty() || !termo.matches("[\\d.\\-\\s]+")) {
            return "";
        }
        return termo.replaceAll("\\D", "");
    }

    // '%' e '_' digitados valem como texto (ESCAPE '!' na query).
    private static String escaparCuringas(String termo) {
        return termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private void validarChefe() {
        String login = simulacaoLogin.loginAtual();
        Usuario u = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuário logado não encontrado"));
        if (u.getPerfil() != PerfilUsuario.CHEFE_FUSEX || !Boolean.TRUE.equals(u.getAtivo())) {
            throw new AcessoNegadoException("Acesso restrito ao Chefe do FUSEX");
        }
    }
}
