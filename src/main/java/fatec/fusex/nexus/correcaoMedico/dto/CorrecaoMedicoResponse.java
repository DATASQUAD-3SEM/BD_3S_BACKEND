package fatec.fusex.nexus.correcaoMedico.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Dados da tela "Corrigir pre-guia {numero}" do medico (T14-BE2). */
public record CorrecaoMedicoResponse(
        String numero,
        String status,
        BeneficiarioResumo beneficiario,
        OcsResumo ocsAtual,
        List<ProcedimentoResumo> procedimentos,
        ObservacaoResumo observacao) {

    // idade vai junto (como no T2-BE1) para o front montar "51 a 60 (58 anos)"
    public record BeneficiarioResumo(String nome, String precCp, String faixaEtaria, int idade, String telefone) {

        // Nao registrar telefone em log.
        @Override
        public String toString() {
            return "BeneficiarioResumo[nome=" + nome + ", telefone=***]";
        }
    }

    public record OcsResumo(Long id, String nome, String contrato, String tipo) {}

    public record ProcedimentoResumo(String codigoTuss, String nome) {}

    public record ObservacaoResumo(String texto, LocalDateTime dataRegistro) {}
}
