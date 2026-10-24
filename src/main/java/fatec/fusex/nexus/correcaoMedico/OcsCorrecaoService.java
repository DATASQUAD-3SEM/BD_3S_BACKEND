package fatec.fusex.nexus.correcaoMedico;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.correcaoMedico.dto.OcsCorrecaoResponse;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.ocs.Ocs;

/** T14-BE3: GET /medicos/me/ocs?busca= (cartao "OCS" da tela "Corrigir pre-guia"). */
@Service
public class OcsCorrecaoService {

    private static final int LIMITE_RESULTADOS = 30;

    private final OcsCorrecaoRepository ocsRepository;
    private final MedicoFusexService medicoService;

    public OcsCorrecaoService(OcsCorrecaoRepository ocsRepository, MedicoFusexService medicoService) {
        this.ocsRepository = ocsRepository;
        this.medicoService = medicoService;
    }

    @Transactional(readOnly = true)
    public OcsCorrecaoResponse buscar(String login, String busca) {
        medicoService.validarPerfilMedico(login); // 403 se nao for medico ativo

        Pageable pageable = PageRequest.of(0, LIMITE_RESULTADOS, Sort.by("nome").ascending());
        Page<Ocs> pagina = (busca == null || busca.isBlank())
                ? ocsRepository.listarAtivas(pageable)
                : ocsRepository.buscarAtivasPorTermo(escaparCuringas(busca.trim()), pageable);

        List<OcsCorrecaoResponse.Item> resultados = pagina.getContent().stream()
                .map(OcsCorrecaoResponse.Item::de)
                .toList();
        // total = resultados antes do corte de 30, para o rodape "Mostrando {n} de {total}"
        return new OcsCorrecaoResponse(resultados, pagina.getTotalElements());
    }

    // '%' e '_' digitados valem como texto (ESCAPE '!' na query).
    private static String escaparCuringas(String termo) {
        return termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
