package fatec.fusex.nexus.preguia.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados recebidos no POST /pre-guias (multipart/form-data), ligados via @ModelAttribute.
 * Nomes dos campos batem com o front (features/preguia/api.ts).
 */
public class CriarPreGuiaRequest {

    @NotBlank(message = "cpf é obrigatorio")
    private String cpf;

    @NotBlank(message = "precCp é obrigatorio")
    private String precCp;

    @NotNull(message = "ocsId é obrigatorio")
    private Long ocsId;

    private MultipartFile arquivo;

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getPrecCp() { return precCp; }
    public void setPrecCp(String precCp) { this.precCp = precCp; }

    public Long getOcsId() { return ocsId; }
    public void setOcsId(Long ocsId) { this.ocsId = ocsId; }

    public MultipartFile getArquivo() { return arquivo; }
    public void setArquivo(MultipartFile arquivo) { this.arquivo = arquivo; }
}