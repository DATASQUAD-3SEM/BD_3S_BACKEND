package fatec.fusex.nexus.chefeFusex;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.chefeFusex.dto.MedicoChefeItemResponse;
import fatec.fusex.nexus.chefeFusex.dto.MedicoChefePaginaResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@Service
public class ChefeMedicoService {

    private static final int SIZE_MAXIMO = 100;

    private final MedicoFusexRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public ChefeMedicoService(MedicoFusexRepository medicoRepository,
                              UsuarioRepository usuarioRepository,
                              SimulacaoLogin simulacaoLogin) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    // T8-BE1: GET /chefe/medicos. Perfil (403) vem antes da validacao dos parametros (400).
    @Transactional(readOnly = true)
    public MedicoChefePaginaResponse listar(String busca, String situacao, int page, int size) {
        validarChefe();

        if (page < 0) {
            throw new RegraDeNegocioException("page deve ser >= 0");
        }
        if (size < 1 || size > SIZE_MAXIMO) {
            throw new RegraDeNegocioException("size deve estar entre 1 e " + SIZE_MAXIMO);
        }

        Boolean ativo = filtroDeSituacao(situacao);
        String termo = busca == null ? "" : busca.trim();

        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<MedicoFusex> pagina = medicoRepository.buscarParaChefe(
                ativo, escaparCuringas(termo), digitosDaBusca(termo), pageable);

        List<MedicoChefeItemResponse> itens = pagina.getContent().stream()
                .map(MedicoChefeItemResponse::de)
                .toList();

        return new MedicoChefePaginaResponse(
                pagina.getTotalElements(),
                pagina.getNumber(),
                pagina.getSize(),
                itens);
    }

    private void validarChefe() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException("Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.CHEFE_FUSEX || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AcessoNegadoException("Acesso restrito ao chefe do FUSEX");
        }
    }

    // ativos (padrao) -> true; inativos -> false; todos -> null (sem filtro)
    private static Boolean filtroDeSituacao(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return true;
        }
        return switch (situacao.trim().toLowerCase()) {
            case "ativos" -> true;
            case "inativos" -> false;
            case "todos" -> null;
            default -> throw new RegraDeNegocioException("situacao invalida: use ativos, inativos ou todos");
        };
    }

    // Busca com cara de CPF/Prec CP (so digitos, pontos, hifen e espacos): compara so os digitos,
    // assim "111.444.777-35" encontra o CPF gravado como 11144477735.
    private static String digitosDaBusca(String termo) {
        if (termo.isEmpty() || !termo.matches("[\\d.\\-\\s]+")) {
            return "";
        }
        return termo.replaceAll("\\D", "");
    }

    // Mesmo tratamento do OcsService: '%' e '_' digitados valem como texto (ESCAPE '!' na query).
    private static String escaparCuringas(String termo) {
        return termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
