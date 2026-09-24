package fatec.fusex.nexus.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato para guardar arquivos (encaminhamento medico em foto/PDF).
 * Quem precisar salvar arquivo usa ESTA interface. Assim, se a decisao de storage mudar
 * (disco -> S3, por exemplo), so a implementacao muda.
 */
public interface ArmazenamentoArquivo {

    /**
     * Salva o arquivo e devolve o caminho relativo (ex: "encaminhamentos/3f2a...pdf").
     * Este caminho e o que vai para pre_guia.encaminhamento_url.
     */
    String salvar(MultipartFile arquivo, String pasta);

    /** Apaga um arquivo salvo antes. Se nao existir, nao faz nada. */
    void remover(String caminhoRelativo);
}
