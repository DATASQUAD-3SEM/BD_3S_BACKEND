package fatec.fusex.nexus.MinhasPreGuias;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import fatec.fusex.nexus.MinhasPreGuias.dto.MinhaPreGuiaResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MinhasPreGuiasService {

    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final PreGuiaHistoricoRepository historicoRepository;
    private final GuiaRepository guiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimulacaoLogin simulacaoLogin;

    public MinhasPreGuiasService(PreGuiaRepository preGuiaRepository,
                                 BeneficiarioRepository beneficiarioRepository,
                                 PreGuiaHistoricoRepository historicoRepository,
                                 GuiaRepository guiaRepository,
                                 UsuarioRepository usuarioRepository,
                                 SimulacaoLogin simulacaoLogin) {
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.historicoRepository = historicoRepository;
        this.guiaRepository = guiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @Transactional(readOnly = true)
    public List<MinhaPreGuiaResponse> listar() {
        Beneficiario beneficiario = beneficiarioLogado();

        List<PreGuia> preGuias = preGuiaRepository
                .findByBeneficiarioUsuarioIdOrderByDataCriacaoDesc(beneficiario.getUsuarioId());

        return preGuias.stream().map(preGuia -> {

            String obs = null;
            if (preGuia.getStatus() == PreGuiaStatus.ATENCAO) {
                obs = historicoRepository
                        .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(
                                preGuia.getIdPreGuia(), PreGuiaStatus.ATENCAO)
                        .map(PreGuiaHistorico::getObservacao)
                        .orElse(null);
            }

            MinhaPreGuiaResponse.GuiaDto guiaDto = null;
            if (preGuia.getStatus() == PreGuiaStatus.CONCLUIDA) {
                guiaDto = guiaRepository.findByPreGuiaIdPreGuia(preGuia.getIdPreGuia())
                        .map(guia -> new MinhaPreGuiaResponse.GuiaDto(
                                guia.getNumero(),
                                guia.getDataInsercao().toLocalDate().plusMonths(2)
                        ))
                        .orElse(null);
            }

            return MinhaPreGuiaResponse.fromEntity(preGuia, obs, guiaDto);

        }).toList();
    }

    private Beneficiario beneficiarioLogado() {
        String login = simulacaoLogin.loginAtual();

        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new NaoAutenticadoException(
                        "Usuario logado nao encontrado"));

        if (usuario.getPerfil() != PerfilUsuario.BENEFICIARIO) {
            throw new AcessoNegadoException(
                    "Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO");
        }

        return beneficiarioRepository.findById(usuario.getIdUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Beneficiario nao encontrado"));
    }
}