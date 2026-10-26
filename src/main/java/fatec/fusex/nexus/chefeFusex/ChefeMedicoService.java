package fatec.fusex.nexus.chefeFusex;

import static fatec.fusex.nexus.common.util.TextoBusca.escaparCuringas;
import static fatec.fusex.nexus.common.util.TextoBusca.digitosDaBusca;
import static fatec.fusex.nexus.common.util.TextoBusca.filtroDeSituacao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.chefeFusex.dto.MedicoChefeItemResponse;
import fatec.fusex.nexus.chefeFusex.dto.MedicoChefePaginaResponse;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;

/**
 * Listagem de medicos para o Chefe (T8-BE1). A busca aceita nome, CPF, Prec CP ou CRM.
 */
@Service
public class ChefeMedicoService {

    private static final int SIZE_MAXIMO = 100;

    private final MedicoFusexRepository medicoRepository;
    private final UsuarioLogado usuarioLogado;

    public ChefeMedicoService(MedicoFusexRepository medicoRepository,
                              UsuarioLogado usuarioLogado) {
        this.medicoRepository = medicoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // T8-BE1: GET /chefe/medicos. Perfil (403) vem antes da validacao dos parametros (400).
    @Transactional(readOnly = true)
    public MedicoChefePaginaResponse listar(String busca, String situacao, int page, int size) {
        usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX);

        if (page < 0) {
            throw new RegraDeNegocioException("page deve ser >= 0");
        }
        if (size < 1 || size > SIZE_MAXIMO) {
            throw new RegraDeNegocioException("size deve estar entre 1 e " + SIZE_MAXIMO);
        }

        Boolean ativo = filtroDeSituacao(situacao);
        String termo = busca == null ? "" : busca.trim();

        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<MedicoFusex> pagina = medicoRepository.buscarParaChefe(
                ativo, escaparCuringas(termo), digitosDaBusca(termo), pageable);

        List<MedicoChefeItemResponse> itens = pagina.getContent().stream()
                .map(MedicoChefeItemResponse::de)
                .toList();

        return new MedicoChefePaginaResponse(
                pagina.getTotalElements(),
                pagina.getNumber(),
                pagina.getSize(),
                itens);
    }
}
