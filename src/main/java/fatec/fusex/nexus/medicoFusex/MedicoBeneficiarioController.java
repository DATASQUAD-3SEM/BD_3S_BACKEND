package fatec.fusex.nexus.medicoFusex;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioService;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;

/**
 * GET /medicos/beneficiarios?precCp= (T2-BE1, T30-BE7): o medico busca o beneficiario pelo Prec CP durante a consulta.
 * Sem beneficiario com esse Prec CP, procura um dependente vigente (T30-BE7); dependente com vigencia vencida: 404.
 * A busca do dependente roda numa transacao somente leitura: o titular e o responsavel sao carregados sob demanda e,
 * com open-in-view desligado, nao podem ser lidos fora dela.
 */
@RestController
@RequestMapping("/medicos/beneficiarios")
public class MedicoBeneficiarioController {

    private final BeneficiarioService beneficiarioService;
    private final SimulacaoLogin simulacaoLogin;
    private final MedicoFusexService medicoService;
    private final DependenteRepository dependenteRepository;
    private final ResponsavelExternoRepository responsavelExternoRepository;
    private final TransactionTemplate transacaoLeitura;

    public MedicoBeneficiarioController(BeneficiarioService beneficiarioService,
                                        SimulacaoLogin simulacaoLogin,
                                        MedicoFusexService medicoService,
                                        DependenteRepository dependenteRepository,
                                        ResponsavelExternoRepository responsavelExternoRepository,
                                        PlatformTransactionManager transactionManager) {
        this.beneficiarioService = beneficiarioService;
        this.simulacaoLogin = simulacaoLogin;
        this.medicoService = medicoService;
        this.dependenteRepository = dependenteRepository;
        this.responsavelExternoRepository = responsavelExternoRepository;
        this.transacaoLeitura = new TransactionTemplate(transactionManager);
        this.transacaoLeitura.setReadOnly(true);
    }

    @GetMapping
    public BeneficiarioMedicoResponse buscarPorPrecCp(@RequestParam(required = false) String precCp) {
        //Pega o login do Header
        String login = simulacaoLogin.loginAtual();
        //Valida se é médico ativo (lança 403 se não for)
        medicoService.validarAcessoMedico(login);
        try {
            return BeneficiarioMedicoResponse.de(beneficiarioService.buscarAtivoPorPrecCp(precCp));
        } catch (RecursoNaoEncontradoException e) {
            return transacaoLeitura.execute(status -> buscarDependentePorPrecCp(precCp)).orElseThrow(() -> e);
        }
    }

    private Optional<BeneficiarioMedicoResponse> buscarDependentePorPrecCp(String precCp) {
        String digitos = precCp == null ? "" : precCp.replaceAll("[^0-9]", "");
        if (digitos.isEmpty()) {
            return Optional.empty();
        }
        LocalDate hoje = Beneficiario.hoje();
        return dependenteRepository.findByPrecCp(digitos)
                .filter(d -> d.getVigenciaFim() == null || !d.getVigenciaFim().isBefore(hoje))
                .map(d -> BeneficiarioMedicoResponse.deDependente(d, hoje, responsavel(d)));
    }

    /** Quem age pelo menor: o titular, no menor sob sua guarda; o responsavel externo, no menor sob responsavel. */
    private String responsavel(Dependente d) {
        if (d.getTipo() == TipoDependente.MENOR_TITULAR) {
            return d.getTitular().getNome();
        }
        if (d.getTipo() == TipoDependente.MENOR_RESPONSAVEL && d.getResponsavel() != null) {
            return responsavelExternoRepository.findById(d.getResponsavel().getIdUsuario())
                    .map(r -> r.getNome())
                    .orElse(null);
        }
        return null;
    }
}
