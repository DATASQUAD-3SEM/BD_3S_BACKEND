package fatec.fusex.nexus.guiaAssinatura;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.guiaAssinatura.dto.GuiaParaAssinarResponse;

@RestController
@RequestMapping("/guias")
public class GuiaAssinaturaController {

    private final GuiaAssinaturaService guiaAssinaturaService;

    public GuiaAssinaturaController(GuiaAssinaturaService guiaAssinaturaService) {
        this.guiaAssinaturaService = guiaAssinaturaService;
    }

    @GetMapping("/para-assinar")
    public List<GuiaParaAssinarResponse> listarParaAssinar() {
        return guiaAssinaturaService.listarParaAssinar();
    }
}