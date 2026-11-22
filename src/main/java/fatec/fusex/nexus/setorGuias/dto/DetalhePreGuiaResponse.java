package fatec.fusex.nexus.setorGuias.dto;

import java.util.List;

/**
 * Resposta do detalhe da pre-guia para o Setor de Guias (T4-BE1).
 * Procedimentos e medico criador so vem na origem MEDICO; encaminhamento so vem na origem BENEFICIARIO.
 */
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

    public record BeneficiarioDto(String nome, String cpf, String precCp, String faixaEtaria) {
        // O toString omite o CPF para que ele nao apareca em log.
        @Override
        public String toString() {
            return "BeneficiarioDto[nome=" + nome + ", precCp=" + precCp + ", faixaEtaria=" + faixaEtaria + "]";
        }
    }

    public record ProcedimentoDto(String codigoTuss, String nome) {}

    /** Medico que criou a pre-guia (regra 3.2): patente, nome, CRM e Prec CP (so digitos). */
    public record MedicoCriadorDto(String patente, String nome, String crm, String precCp) {}

    public record EncaminhamentoDto(String nome, String tipo, long tamanhoBytes) {}

    // exames: na ordem gravada; lista vazia quando o rascunho e anterior aos exames (T22-BE1)
    public record GuiaRascunhoDto(String numero, String nomeArquivo, List<ExameDto> exames) {}

    public record ExameDto(String codDgp, String descricao, Integer quantidade) {}
}