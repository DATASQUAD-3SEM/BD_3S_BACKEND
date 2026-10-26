package fatec.fusex.nexus.preGuia.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * Dados recebidos no POST /pre-guias (multipart/form-data), ligados via @ModelAttribute.
 * O beneficiario nao vem na requisicao: ele e sempre o usuario logado. As validacoes de OCS e arquivo
 * ficam no PreGuiaService, que devolve 422 apontando o campo.
 */
public class CriarPreGuiaRequest {

    private Long ocsId;

    private MultipartFile arquivo;

    public Long getOcsId() { return ocsId; }
    public void setOcsId(Long ocsId) { this.ocsId = ocsId; }

    public MultipartFile getArquivo() { return arquivo; }
    public void setArquivo(MultipartFile arquivo) { this.arquivo = arquivo; }
}
