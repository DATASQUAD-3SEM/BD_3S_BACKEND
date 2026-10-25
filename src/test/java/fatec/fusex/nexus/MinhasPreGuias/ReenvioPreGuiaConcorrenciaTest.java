package fatec.fusex.nexus.MinhasPreGuias;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Reenvio duplo: dois reenvios ao mesmo tempo geram uma unica mudanca (um 200 e um 409).
 * Grava de verdade no H2 (sem @Transactional no teste); o @AfterEach apaga tudo o que foi criado.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReenvioPreGuiaConcorrenciaTest {

    private static final String BENEFICIARIO = "70000000401";
    private static final String SETOR = "setor.reenvio";

    private final ReenvioPreGuiaService service;
    private final SimulacaoLogin simulacaoLogin;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    private Long idPreGuia;
    private Long idBeneficiario;
    private Long idOcsAtual;
    private Long idOcsNova;

    ReenvioPreGuiaConcorrenciaTest(
            ReenvioPreGuiaService service,
            SimulacaoLogin simulacaoLogin,
            UsuarioRepository usuarioRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            PreGuiaHistoricoRepository historicoRepository) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
    }

    @BeforeEach
    void dados() {
        Usuario setor = usuario(SETOR, PerfilUsuario.SETOR_GUIAS);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Beneficiario Reenvio");
        beneficiario.setPrecCp("70000000422");
        beneficiario.setDataNascimento(LocalDate.of(1968, 1, 1));
        beneficiario.setTelefone("12999990000");
        idBeneficiario = beneficiarioRepository.saveAndFlush(beneficiario).getUsuarioId();

        idOcsAtual = ocs("OCS Reenvio Atual", "CONC-REENVIO-1");
        idOcsNova = ocs("OCS Reenvio Nova", "CONC-REENVIO-2");

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocsRepository.findById(idOcsAtual).orElseThrow());
        preGuia.setStatus(PreGuiaStatus.ATENCAO);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        preGuia = preGuiaRepository.saveAndFlush(preGuia);
        idPreGuia = preGuia.getIdPreGuia();

        historicoRepository.saveAndFlush(new PreGuiaHistorico(
                null, preGuia, PreGuiaStatus.ATENCAO, "Trocar a OCS", null, setor, LocalDateTime.now()));
    }

    @AfterEach
    void limpar() {
        historicoRepository.deleteAll(historicoRepository.findAll().stream()
                .filter(h -> h.getPreGuia().getIdPreGuia().equals(idPreGuia)).toList());
        preGuiaRepository.deleteById(idPreGuia);
        ocsRepository.deleteById(idOcsAtual);
        ocsRepository.deleteById(idOcsNova);
        beneficiarioRepository.deleteById(idBeneficiario);
        usuarioRepository.findByLogin(BENEFICIARIO).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(SETOR).ifPresent(usuarioRepository::delete);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Long ocs(String nome, String contrato) {
        Ocs ocs = new Ocs();
        ocs.setNome(nome);
        ocs.setContratoNum(contrato);
        ocs.setTipo(OcsTipo.OCS);
        return ocsRepository.saveAndFlush(ocs).getIdOcs();
    }

    @Test
    void doisReenviosSimultaneos_geramUmaUnicaMudanca() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<?>> envios = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                envios.add(executor.submit(() -> {
                    largada.await();
                    simulacaoLogin.definir(BENEFICIARIO); // o login fica no ThreadLocal de cada thread
                    try {
                        return service.reenviar(idPreGuia, idOcsNova, null);
                    } finally {
                        simulacaoLogin.limpar();
                    }
                }));
            }
            largada.countDown(); // solta as duas threads ao mesmo tempo

            int sucessos = 0;
            int conflitos = 0;
            for (Future<?> envio : envios) {
                try {
                    envio.get(15, TimeUnit.SECONDS);
                    sucessos++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ConflitoException.class);
                    conflitos++;
                }
            }

            assertThat(sucessos).isEqualTo(1);
            assertThat(conflitos).isEqualTo(1);
            long reenvios = historicoRepository.findAll().stream()
                    .filter(h -> h.getPreGuia().getIdPreGuia().equals(idPreGuia))
                    .filter(h -> h.getStatus() == PreGuiaStatus.PENDENTE)
                    .count();
            assertThat(reenvios).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
