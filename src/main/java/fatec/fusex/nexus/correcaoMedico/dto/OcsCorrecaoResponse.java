package fatec.fusex.nexus.correcaoMedico.dto;

import java.util.List;

import fatec.fusex.nexus.ocs.Ocs;

/** Resposta do GET /medicos/me/ocs (T14-BE3): mesmo formato JSON do GET /ocs, com DTO proprio. */
public record OcsCorrecaoResponse(
        List<Item> resultados,
        long total) {

    public record Item(Long id, String nome, String contrato, String tipo) {

        public static Item de(Ocs ocs) {
            return new Item(ocs.getIdOcs(), ocs.getNome(), ocs.getContratoNum(), ocs.getTipo().name());
        }
    }
}
