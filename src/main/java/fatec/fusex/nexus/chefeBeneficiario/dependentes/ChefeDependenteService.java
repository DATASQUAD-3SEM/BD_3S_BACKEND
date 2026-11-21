package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.usuario.PerfilUsuario;

/** Dependentes de um titular, na visao do chefe do FUSEX (T31-BE1). Somente leitura. */
@Service
public class ChefeDependenteService {

    private static final int PREFIXO_PREC_CP = 9;
    private static final int SEQUENCIA_MAXIMA = 99;

    private final UsuarioLogado usuarioLogado;
    private final BeneficiarioRepository beneficiarioRepository;
    private final DependenteRepository dependenteRepository;
    private final ResponsavelExternoRepository responsavelExternoRepository;

    public ChefeDependenteService(UsuarioLogado usuarioLogado,
                                  BeneficiarioRepository beneficiarioRepository,
                                  DependenteRepository dependenteRepository,
                                  ResponsavelExternoRepository responsavelExternoRepository) {
        this.usuarioLogado = usuarioLogado;
        this.beneficiarioRepository = beneficiarioRepository;
        this.dependenteRepository = dependenteRepository;
        this.responsavelExternoRepository = responsavelExternoRepository;
    }

    /** Vigentes primeiro, depois os de vigencia vencida; dentro de cada grupo, por ordem de cadastro. */
    @Transactional(readOnly = true)
    public DependentesDoTitularResponse listarDoTitular(Long titularId) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);
        Beneficiario titular = buscarTitular(titularId);
        LocalDate hoje = Beneficiario.hoje();

        List<DependenteItemResponse> itens = new ArrayList<>(
                dependenteRepository.findByTitularUsuarioIdOrderByIdDependenteAsc(titularId).stream()
                        .map(d -> DependenteItemResponse.de(d, hoje, responsaveis(d, titular)))
                        .toList());
        itens.sort(Comparator.comparing(DependenteItemResponse::vigenciaVencida));

        return new DependentesDoTitularResponse(titular.getUsuarioId(), titular.getNome(), itens);
    }

    /**
     * Proximo Prec CP livre da base do titular: os 9 primeiros digitos do titular seguidos de uma sequencia de 2
     * digitos, a partir de 01 (o 00 e do titular, como nos exemplos do cadastro).
     */
    @Transactional(readOnly = true)
    public ProximoPrecCpResponse proximoPrecCp(Long titularId) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);
        Beneficiario titular = buscarTitular(titularId);
        String base = titular.getPrecCp().substring(0, PREFIXO_PREC_CP);

        Set<String> ocupados = new HashSet<>();
        for (Dependente d : dependenteRepository.findByPrecCpStartingWith(base)) {
            ocupados.add(d.getPrecCp());
        }
        for (int seq = 1; seq <= SEQUENCIA_MAXIMA; seq++) {
            String candidato = base + String.format("%02d", seq);
            if (!ocupados.contains(candidato)) {
                return new ProximoPrecCpResponse(candidato);
            }
        }
        throw new RecursoNaoEncontradoException("Nao ha Prec CP livre na base deste titular");
    }

    private Beneficiario buscarTitular(Long titularId) {
        return beneficiarioRepository.findById(titularId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Beneficiario nao encontrado"));
    }

    /** TITULAR no menor sob guarda do titular; EXTERNO no menor sob responsavel externo; nenhum no maior. */
    private List<ResponsavelDependenteResponse> responsaveis(Dependente d, Beneficiario titular) {
        if (d.getTipo() == TipoDependente.MENOR_TITULAR) {
            return List.of(new ResponsavelDependenteResponse(titular.getUsuarioId(), titular.getNome(), "TITULAR"));
        }
        if (d.getTipo() == TipoDependente.MENOR_RESPONSAVEL && d.getResponsavel() != null) {
            return responsavelExternoRepository.findById(d.getResponsavel().getIdUsuario())
                    .map(r -> List.of(new ResponsavelDependenteResponse(r.getUsuarioId(), r.getNome(), "EXTERNO")))
                    .orElse(List.of());
        }
        return List.of();
    }
}
