package fatec.fusex.nexus.ocs;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.procedimento.ProcedimentoExame;

@RestController
@RequestMapping("/ocs")
public class OcsController {

    private final OcsService ocsService;

    public OcsController(OcsService ocsService) {
        this.ocsService = ocsService;
    }

    @GetMapping
    public List<Ocs> listar() {
        return ocsService.listar();
    }

    @GetMapping("/{id}/procedimentos")
    public List<ProcedimentoExame> listarProcedimentos(@PathVariable Long id) {
        return ocsService.buscarPorId(id)
                .getProcedimentos()
                .stream()
                .toList();
    }
}