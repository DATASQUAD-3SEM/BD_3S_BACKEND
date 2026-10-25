package fatec.fusex.nexus.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ArmazenamentoArquivo {

    String salvar(MultipartFile arquivo, String pasta);

    void remover(String caminhoRelativo);

    Resource carregar(String caminhoRelativo);

    /**
     * Remove o arquivo ignorando falhas. Usado na limpeza apos um erro (ou apos trocar um arquivo),
     * para que a falha ao apagar nao esconda o resultado ou a causa principal da operacao.
     */
    default void removerSemFalhar(String caminhoRelativo) {
        try {
            remover(caminhoRelativo);
        } catch (RuntimeException e) {
            // ignorado de proposito: a limpeza e apenas uma tentativa
        }
    }
}