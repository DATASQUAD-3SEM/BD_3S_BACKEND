package fatec.fusex.nexus.setorGuias.dto;

import java.util.List;

public record DetalhePreGuiaResponse(
        Long id,
        String numero,
        String status,
        String origem,
        BeneficiarioDto beneficiario,
        String ocsNome,
        List<ProcedimentoDto> procedimentos,
        MedicoCriadorDto medicoCriador,
        EncaminhamentoDto encaminhamento,
        GuiaRascunhoDto guiaRascunho,
        boolean guiaConcluida) {

    public record BeneficiarioDto(String nome, String precCp, String faixaEtaria) {}

    public record ProcedimentoDto(String codigoTuss, String nome) {}

    public record MedicoCriadorDto(String patente, String nome, String crm) {}

    public record EncaminhamentoDto(String nome, String tipo, long tamanhoBytes) {}

    public record GuiaRascunhoDto(String numero, String nomeArquivo) {}
}