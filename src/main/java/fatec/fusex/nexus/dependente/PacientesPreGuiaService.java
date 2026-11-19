package fatec.fusex.nexus.dependente;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Quem o usuario pode escolher como paciente da pre-guia (T30-BE2).
 *
 * O proprio usuario entra quando e titular (tem Beneficiario, sem dependente ativo proprio) ou quando e um dependente
 * maior com conta propria e vigencia ativa. Os menores entram quando o usuario tem ResponsavelLegal ativo com eles
 * e estao ativos e dentro da vigencia hoje. Dependente encerrado, ou que fez 18 anos, nao entra.
 */
@Service
public class PacientesPreGuiaService {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final BeneficiarioRepository beneficiarioRepository;
    private final DependenteRepository dependenteRepository;
    private final ResponsavelLegalRepository responsavelLegalRepository;

    public PacientesPreGuiaService(BeneficiarioRepository beneficiarioRepository,
                                   DependenteRepository dependenteRepository,
                                   ResponsavelLegalRepository responsavelLegalRepository) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.dependenteRepository = dependenteRepository;
        this.responsavelLegalRepository = responsavelLegalRepository;
    }

    /** Data de hoje no fuso de Sao Paulo: base para menor de idade e vigencia. */
    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    /** Pacientes escolhiveis pelo usuario: ele mesmo (se for o caso) e os menores que acompanha. */
    @Transactional(readOnly = true)
    public List<PacienteResponse> listar(Usuario usuario) {
        LocalDate hoje = hoje();
        List<PacienteResponse> pacientes = new ArrayList<>();

        Optional<Dependente> proprio = dependentePropio(usuario, hoje);
        if (proprio.isPresent()) {
            pacientes.add(de(proprio.get()));
        } else {
            beneficiarioRepository.findById(usuario.getIdUsuario())
                    .map(Beneficiario::getNome)
                    .ifPresent(nome -> pacientes.add(new PacienteResponse(null, nome, "Titular")));
        }

        menoresAcompanhados(usuario, hoje).stream()
                .sorted(Comparator.comparing(Dependente::getNome))
                .map(PacientesPreGuiaService::de)
                .forEach(pacientes::add);
        return pacientes;
    }

    /**
     * Devolve o dependente se o usuario pode criar pre-guia para ele. Senao, 403 (inclui dependente encerrado ou
     * que nao e menor acompanhado pelo usuario).
     */
    @Transactional(readOnly = true)
    public Dependente dependentePermitido(Usuario usuario, Long dependenteId) {
        LocalDate hoje = hoje();
        boolean permitido = dependentePropio(usuario, hoje)
                .filter(d -> d.getIdDependente().equals(dependenteId))
                .isPresent()
                || menoresAcompanhados(usuario, hoje).stream().anyMatch(d -> d.getIdDependente().equals(dependenteId));
        if (!permitido) {
            throw new AcessoNegadoException("Acesso negado: este paciente nao esta disponivel para a sua pre-guia");
        }
        return dependenteRepository.findById(dependenteId)
                .orElseThrow(() -> new AcessoNegadoException("Acesso negado: este paciente nao esta disponivel para a sua pre-guia"));
    }

    // Dependente com conta propria, ativo, dentro da vigencia e maior de idade hoje
    private Optional<Dependente> dependentePropio(Usuario usuario, LocalDate hoje) {
        return dependenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .filter(d -> d.vigenteEm(hoje) && !d.menorEm(hoje))
                .findFirst();
    }

    // Menores de idade, ativos e dentro da vigencia, com vinculo de responsavel legal ativo com o usuario
    private List<Dependente> menoresAcompanhados(Usuario usuario, LocalDate hoje) {
        Map<Long, Dependente> porId = new LinkedHashMap<>();
        responsavelLegalRepository.findByUsuarioIdUsuarioAndDataFimIsNull(usuario.getIdUsuario()).stream()
                .map(ResponsavelLegal::getDependente)
                .filter(d -> d.vigenteEm(hoje) && d.menorEm(hoje))
                .forEach(d -> porId.putIfAbsent(d.getIdDependente(), d));
        return new ArrayList<>(porId.values());
    }

    private static PacienteResponse de(Dependente dependente) {
        String relacao = dependente.getVinculo() != null ? dependente.getVinculo() : "Dependente";
        return new PacienteResponse(dependente.getIdDependente(), dependente.getNome(), relacao);
    }
}
