package fatec.fusex.nexus.recepcao;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.recepcao.dto.GuiaRecepcaoResponse;

@RestController
@RequestMapping("/recepcao/guias")
public class RecepcaoGuiasController {

    private final RecepcaoGuiasService recepcaoGuiasService;

    public RecepcaoGuiasController(RecepcaoGuiasService recepcaoGuiasService) {
        this.recepcaoGuiasService = recepcaoGuiasService;
    }

    @GetMapping
    public List<GuiaRecepcaoResponse> buscar(@RequestParam(required = false) String busca) {
        return recepcaoGuiasService.buscar(busca);
    }
}