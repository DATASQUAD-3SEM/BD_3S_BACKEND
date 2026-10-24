package fatec.fusex.nexus.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ArmazenamentoArquivo {


    String salvar(MultipartFile arquivo, String pasta);

    void remover(String caminhoRelativo);


    Resource carregar(String caminhoRelativo);
}