package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimento;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T22-BE1: falha ao gravar um exame nao grava nada (nem o rascunho).
 * Sem @Transactional no teste, para o rollback ser o do proprio service (mesmo padrao de ChefeMedicoCadastroTransacaoTest).
 */
@SpringBootTest
@ActiveProfiles("test")
class AnexarGuiaRascunhoTransacaoTest {

    private static final String SETOR = "setor.rollback";
    private static final String BENEFICIARIO = "benef.rollback";
    private static final String EXAMES = "[{\"codDgp\":\"41001109\",\"descricao\":\"TC - Abdome superior\",\"quantidade\":1}]";

    private final AnexarGuiaService service;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;
    private final GuiaRascunhoProcedimentoRepository procedimentoRepository;
    private final SimulacaoLogin simulacaoLogin;

    @MockitoSpyBean
    private GuiaRascunhoProcedimentoRepository procedimentoRepositorySpy;

    private Beneficiario beneficiario;
    private Ocs ocs;
    private PreGuia preGuia;

    AnexarGuiaRascunhoTransacaoTest(
            AnexarGuiaService service,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            GuiaRascunhoRepository guiaRascunhoRepository,
            GuiaRascunhoProcedimentoRepository procedimentoRepository,
            SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @BeforeEach
    void dados() {
        usuario(SETOR, PerfilUsuario.SETOR_GUIAS);
        simulacaoLogin.definir(SETOR);

        Usuario usuarioBeneficiario = usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioBeneficiario);
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("98765432109");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);

        ocs = new Ocs();
        ocs.setNome("Laboratorio Rollback");
        ocs.setContratoNum("ROLLBACK-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.saveAndFlush(ocs);

        preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(PreGuiaStatus.EM_ANALISE);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        preGuiaRepository.saveAndFlush(preGuia);
    }

    @AfterEach
    void limpar() {
        simulacaoLogin.limpar();
        guiaRascunhoRepository.findById(preGuia.getIdPreGuia()).ifPresent(guiaRascunhoRepository::delete);
        preGuiaRepository.findById(preGuia.getIdPreGuia()).ifPresent(preGuiaRepository::delete);
        beneficiarioRepository.findById(beneficiario.getUsuario().getIdUsuario())
                .ifPresent(beneficiarioRepository::delete);
        usuarioRepository.findByLogin(BENEFICIARIO).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(SETOR).ifPresent(usuarioRepository::delete);
        ocsRepository.findById(ocs.getIdOcs()).ifPresent(ocsRepository::delete);
    }

    @Test
    void falhaAoGravarUmExame_naoGravaORascunhoNemOsExames() {
        doThrow(new IllegalStateException("falha simulada no insert do exame"))
                .when(procedimentoRepositorySpy).saveAllAndFlush(any());

        MockMultipartFile pdf = new MockMultipartFile("pdf", "guia.pdf", "application/pdf", "%PDF-1.4 guia".getBytes());

        assertThatThrownBy(() -> service.anexar(preGuia.getIdPreGuia(), "2026001234", pdf, EXAMES))
                .isInstanceOf(IllegalStateException.class);

        assertThat(guiaRascunhoRepository.findById(preGuia.getIdPreGuia())).isEmpty();
        assertThat(procedimentoRepository.listarDaPreGuia(preGuia.getIdPreGuia()))
                .isEmpty();
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
