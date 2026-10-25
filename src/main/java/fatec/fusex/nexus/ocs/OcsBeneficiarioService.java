package fatec.fusex.nexus.ocs;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.ocs.dto.OcsResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;

@Service
public class OcsBeneficiarioService {

    static final int LIMITE_RESULTADOS = 30;

    private final OcsRepository ocsRepository;
    private final UsuarioLogado usuarioLogado;

    public OcsBeneficiarioService(OcsRepository ocsRepository,
                                  UsuarioLogado usuarioLogado) {
        this.ocsRepository = ocsRepository;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public List<OcsResponse> buscarAtivas(String busca) {
        usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO);

        String termo = normalizar(busca);

        return ocsRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(ocs -> termo.isEmpty() || corresponde(ocs, termo))
                .limit(LIMITE_RESULTADOS)
                .map(OcsResponse::de)
                .toList();
    }

    private static boolean corresponde(Ocs ocs, String termo) {
        return normalizar(ocs.getNome()).contains(termo)
                || normalizar(ocs.getContratoNum()).contains(termo)
                || normalizar(ocs.getTipo().name()).contains(termo);
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}