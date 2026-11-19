package fatec.fusex.nexus.preGuia.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * Dados recebidos no POST /pre-guias (multipart/form-data), ligados via @ModelAttribute.
 * O beneficiario nao vem na requisicao: ele e sempre o usuario logado. dependenteId (T30-BE2) escolhe o paciente
 * quando nao e o proprio titular. As validacoes de OCS, arquivo e paciente ficam no PreGuiaService, que devolve
 * 422 apontando o campo (403 para paciente que o usuario nao pode escolher).
 */
public class CriarPreGuiaRequest {

    private Long ocsId;

    private Long dependenteId;

    private MultipartFile arquivo;

    public Long getOcsId() { return ocsId; }
    public void setOcsId(Long ocsId) { this.ocsId = ocsId; }

    public Long getDependenteId() { return dependenteId; }
    public void setDependenteId(Long dependenteId) { this.dependenteId = dependenteId; }

    public MultipartFile getArquivo() { return arquivo; }
    public void setArquivo(MultipartFile arquivo) { this.arquivo = arquivo; }
}
