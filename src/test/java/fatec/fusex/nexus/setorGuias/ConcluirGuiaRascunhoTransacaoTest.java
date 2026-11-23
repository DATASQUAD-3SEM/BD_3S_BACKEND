package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimento;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoProcedimentoId;
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
 * T22-BE2: falha ao gravar um exame da Guia nao grava nada (nem a Guia).
 * Sem @Transactional no teste, para o rollback ser o do proprio service (mesmo padrao de AnexarGuiaRascunhoTransacaoTest).
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcluirGuiaRascunhoTransacaoTest {

    private static final String SETOR = "setor.concluir.rollback";
    private static final String BENEFICIARIO = "benef.concluir.rollback";

    private final ConcluirGuiaService service;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;
    private final GuiaRascunhoProcedimentoRepository guiaRascunhoProcedimentoRepository;
    private final SimulacaoLogin simulacaoLogin;

    @MockitoSpyBean
    private GuiaProcedimentoRepository guiaProcedimentoRepositorySpy;

    private Usuario usuarioBeneficiario;
    private Beneficiario beneficiario;
    private Ocs ocs;
    private PreGuia preGuia;

    ConcluirGuiaRascunhoTransacaoTest(
            ConcluirGuiaService service,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            GuiaRepository guiaRepository,
            GuiaRascunhoRepository guiaRascunhoRepository,
            GuiaRascunhoProcedimentoRepository guiaRascunhoProcedimentoRepository,
            SimulacaoLogin simulacaoLogin) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
        this.guiaRascunhoProcedimentoRepository = guiaRascunhoProcedimentoRepository;
        this.simulacaoLogin = simulacaoLogin;
    }

    @BeforeEach
    void dados() {
        usuario(SETOR, PerfilUsuario.SETOR_GUIAS);
        simulacaoLogin.definir(SETOR);

        usuarioBeneficiario = usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioBeneficiario);
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("98765432108");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);

        ocs = new Ocs();
        ocs.setNome("Laboratorio Rollback Concluir");
        ocs.setContratoNum("ROLLBACK-002");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.saveAndFlush(ocs);

        preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(PreGuiaStatus.EM_ANALISE);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        preGuiaRepository.saveAndFlush(preGuia);

        GuiaRascunho rascunho = new GuiaRascunho();
        rascunho.setPreGuia(preGuia);
        rascunho.setNumero("2026007777");
        rascunho.setArquivoUrl("guias-rascunho/2026007777.pdf");
        rascunho.setNomeArquivo("guia-2026007777.pdf");
        rascunho.setDataAtualizacao(LocalDateTime.now());
        rascunho.setValidadeAte(LocalDate.of(2099, 1, 1));
        guiaRascunhoRepository.saveAndFlush(rascunho);

        guiaRascunhoProcedimentoRepository.saveAllAndFlush(List.of(
                new GuiaRascunhoProcedimento(new GuiaRascunhoProcedimentoId(preGuia.getIdPreGuia(), 1),
                        "41001109", "TC - Abdome superior", 1),
                new GuiaRascunhoProcedimento(new GuiaRascunhoProcedimentoId(preGuia.getIdPreGuia(), 2),
                        "50011015", "Consulta pediatra", 2)));
    }

    @AfterEach
    void limpar() {
        simulacaoLogin.limpar();
        guiaRascunhoProcedimentoRepository.deleteAll(
                guiaRascunhoProcedimentoRepository.listarDaPreGuia(preGuia.getIdPreGuia()));
        guiaRascunhoRepository.findById(preGuia.getIdPreGuia()).ifPresent(guiaRascunhoRepository::delete);
        preGuiaRepository.findById(preGuia.getIdPreGuia()).ifPresent(preGuiaRepository::delete);
        beneficiarioRepository.findById(usuarioBeneficiario.getIdUsuario()).ifPresent(beneficiarioRepository::delete);
        usuarioRepository.findByLogin(BENEFICIARIO).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(SETOR).ifPresent(usuarioRepository::delete);
        ocsRepository.findById(ocs.getIdOcs()).ifPresent(ocsRepository::delete);
    }

    @Test
    void falhaAoGravarUmExame_naoGravaAGuiaNemOsExames() {
        doThrow(new IllegalStateException("falha simulada no insert do exame da guia"))
                .when(guiaProcedimentoRepositorySpy).saveAllAndFlush(any());

        assertThatThrownBy(() -> service.concluir(preGuia.getIdPreGuia()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(guiaRepository.findByPreGuiaIdPreGuia(preGuia.getIdPreGuia())).isEmpty();
        assertThat(guiaProcedimentoRepositorySpy.count()).isZero();
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
