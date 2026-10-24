package fatec.fusex.nexus.preguia;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.preguia.dto.CorrecaoPreGuiaResponse;

@RestController
@RequestMapping("/beneficiarios/me/pre-guias")
public class BeneficiarioPreGuiaController {

    private final PreGuiaService preGuiaService;

    public BeneficiarioPreGuiaController(PreGuiaService preGuiaService) {
        this.preGuiaService = preGuiaService;
    }

    @GetMapping("/{id}/correcao")
    @PreAuthorize("hasRole('BENEFICIARIO')")
    public ResponseEntity<CorrecaoPreGuiaResponse> buscarCorrecao(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        // O username no token (ou mock) será o CPF do beneficiário
        return ResponseEntity.ok(preGuiaService.buscarDadosCorrecao(id, userDetails.getUsername()));
    }
}