package fatec.fusex.nexus.dependenteLeitura;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/** T31-BE5 (SCRUM-274): Minhas informacoes para titular, responsavel externo e dependente maior. Somente leitura. */
@Service
public class DependenteLeituraService {

    private static final int MAIORIDADE = 18;

    private final UsuarioLogado usuarioLogado;
    private final DependenteRepository dependenteRepository;

    public DependenteLeituraService(UsuarioLogado usuarioLogado, DependenteRepository dependenteRepository) {
        this.usuarioLogado = usuarioLogado;
        this.dependenteRepository = dependenteRepository;
    }

    /** Dependentes vigentes do titular; podeCriarPreGuia nos menores que ele acompanha (MENOR_TITULAR). */
    @Transactional(readOnly = true)
    public List<DependenteDoTitularResponse> dependentesDoTitular() {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO);
        LocalDate hoje = Beneficiario.hoje();
        return dependenteRepository.findByTitularUsuarioIdOrderByIdDependenteAsc(usuario.getIdUsuario()).stream()
                .filter(d -> vigente(d, hoje))
                .map(d -> new DependenteDoTitularResponse(d.getIdDependente(), d.getNome(), d.getVinculo(),
                        d.getPrecCp(), idade(d, hoje), d.getTipo(), d.getVigenciaFim(),
                        d.getTipo() == TipoDependente.MENOR_TITULAR))
                .toList();
    }

    /** Menores vigentes que o responsavel externo acompanha, com o titular (um so responsavel por menor, regra 13). */
    @Transactional(readOnly = true)
    public List<MenorAcompanhadoResponse> menoresDoResponsavel() {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.RESPONSAVEL_EXTERNO);
        LocalDate hoje = Beneficiario.hoje();
        return dependenteRepository.findByResponsavelIdUsuarioAndTipo(usuario.getIdUsuario(), TipoDependente.MENOR_RESPONSAVEL)
                .stream()
                .filter(d -> vigente(d, hoje) && menor(d, hoje))
                .map(d -> new MenorAcompanhadoResponse(d.getIdDependente(), d.getNome(), d.getVinculo(),
                        d.getPrecCp(), idade(d, hoje), d.getVigenciaFim(), d.getTitular().getNome()))
                .toList();
    }

    /** Cadastro do dependente maior, para ele mesmo: so leitura. */
    @Transactional(readOnly = true)
    public DependenteMeResponse meuCadastro() {
        Usuario usuario = usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO);
        Dependente d = dependenteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException(
                        "Dependente nao encontrado"));
        LocalDate hoje = Beneficiario.hoje();
        return new DependenteMeResponse(d.getNome(), d.getPrecCp(), d.getVinculo(), d.getVigenciaFim(),
                vigente(d, hoje), d.getTitular().getNome(), usuario.getAssinatura() != null);
    }

    private static boolean vigente(Dependente d, LocalDate hoje) {
        return d.getVigenciaFim() == null || !d.getVigenciaFim().isBefore(hoje);
    }

    private static boolean menor(Dependente d, LocalDate hoje) {
        return d.getDataNascimento().plusYears(MAIORIDADE).isAfter(hoje);
    }

    private static int idade(Dependente d, LocalDate hoje) {
        return Period.between(d.getDataNascimento(), hoje).getYears();
    }

    /** dependenteId leva o "Criar pre-guia" ao passo Paciente com o menor ja escolhido (T31-FE4). */
    public record DependenteDoTitularResponse(Long dependenteId, String nome, String vinculo, String precCp, int idade,
                                              TipoDependente tipo, LocalDate vigenciaFim, boolean podeCriarPreGuia) {
    }

    public record MenorAcompanhadoResponse(Long dependenteId, String nome, String vinculo, String precCp, int idade,
                                           LocalDate vigenciaFim, String titularNome) {
    }

    public record DependenteMeResponse(String nome, String precCp, String vinculo, LocalDate vigenciaFim,
                                       boolean vigente, String titularNome, boolean temAssinatura) {
    }
}
