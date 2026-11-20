package fatec.fusex.nexus.rastreabilidade;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fatec.fusex.nexus.rastreabilidade.dto.ItemLisuraRequest;
import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse;

@RestController
@RequestMapping("/rastreabilidade/faturas")
public class LisuraController {

    private final LisuraService service;

    public LisuraController(LisuraService service) {
        this.service = service;
    }

    @PutMapping("/{id}/lisura")
    public FaturaDetalheResponse salvar(@PathVariable Long id, @RequestBody List<ItemLisuraRequest> itens) {
        return service.salvar(id, itens);
    }
}