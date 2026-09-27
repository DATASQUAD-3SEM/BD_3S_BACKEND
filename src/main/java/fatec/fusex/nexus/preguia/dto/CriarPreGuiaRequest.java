package fatec.fusex.nexus.preguia.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Dados recebidos no POST /pre-guias (multipart/form-data), ligados via @ModelAttribute.
 *
 * Os nomes dos campos abaixo tem que bater com os nomes das partes que o front manda
 * (features/preguia/api.ts, docs/CONTRATO_API.md): cpf, precCp, ocsId, procedimentoIds
 * (repetido, um por exame) e arquivo. Nao mude um lado sem mudar o outro.
 */
public class CriarPreGuiaRequest {

    @NotBlank(message = "cpf é obrigatorio")
    private String cpf;

    @NotBlank(message = "precCp é obrigatorio")
    private String precCp;

    @NotNull(message = "ocsId é obrigatorio")
    private Long ocsId;

    @NotEmpty(message = "informe ao menos um procedimento")
    private List<Long> procedimentoIds;

    private MultipartFile arquivo;

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPrecCp() {
        return precCp;
    }

    public void setPrecCp(String precCp) {
        this.precCp = precCp;
    }

    public Long getOcsId() {
        return ocsId;
    }

    public void setOcsId(Long ocsId) {
        this.ocsId = ocsId;
    }

    public List<Long> getProcedimentoIds() {
        return procedimentoIds;
    }

    public void setProcedimentoIds(List<Long> procedimentoIds) {
        this.procedimentoIds = procedimentoIds;
    }

    public MultipartFile getArquivo() {
        return arquivo;
    }

    public void setArquivo(MultipartFile arquivo) {
        this.arquivo = arquivo;
    }
}
