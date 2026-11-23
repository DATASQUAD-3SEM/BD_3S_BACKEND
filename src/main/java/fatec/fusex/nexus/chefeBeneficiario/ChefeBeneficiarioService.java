package fatec.fusex.nexus.chefeBeneficiario;

import static fatec.fusex.nexus.common.util.TextoBusca.escaparCuringas;
import static fatec.fusex.nexus.common.util.TextoBusca.digitosDaBusca;
import static fatec.fusex.nexus.common.util.TextoBusca.filtroDeSituacao;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioItemResponse;
import fatec.fusex.nexus.chefeBeneficiario.dto.BeneficiarioListaResponse;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Listagem de beneficiarios para o Chefe (T7-BE1). A busca aceita nome, CPF ou Prec CP;
 * quando o termo tem formato numerico, a comparacao e feita apenas pelos digitos.
 */
@Service
public class ChefeBeneficiarioService {

    private static final int SIZE_MAXIMO = 100;

    private final BeneficiarioRepository beneficiarioRepository;
    private final fatec.fusex.nexus.dependente.DependenteRepository dependenteRepository;
    private final UsuarioLogado usuarioLogado;

    public ChefeBeneficiarioService(BeneficiarioRepository beneficiarioRepository,
                                    fatec.fusex.nexus.dependente.DependenteRepository dependenteRepository,
                                    UsuarioLogado usuarioLogado) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.dependenteRepository = dependenteRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Perfil (403) vem antes da validacao dos parametros (400)
    @Transactional(readOnly = true)
    public BeneficiarioListaResponse listar(String busca, String situacao, int page, int size) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);

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
        java.util.Map<Long, Long> ativosPorTitular = contarDependentesAtivos(pageResult.getContent());
        return BeneficiarioListaResponse.de(pageResult.map(b ->
                BeneficiarioItemResponse.de(b, ativosPorTitular.getOrDefault(b.getUsuarioId(), 0L).intValue())));
    }

    /** Dependentes vigentes por titular (T31-BE1). */
    private java.util.Map<Long, Long> contarDependentesAtivos(java.util.List<Beneficiario> titulares) {
        if (titulares.isEmpty()) {
            return java.util.Map.of();
        }
        java.time.LocalDate hoje = Beneficiario.hoje();
        return dependenteRepository.findByTitularUsuarioIdIn(titulares.stream().map(Beneficiario::getUsuarioId).toList())
                .stream()
                .filter(d -> d.getVigenciaFim() == null || !d.getVigenciaFim().isBefore(hoje))
                .collect(java.util.stream.Collectors.groupingBy(d -> d.getTitular().getUsuarioId(),
                        java.util.stream.Collectors.counting()));
    }
}
