package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.setorGuias.dto.AberturaPreGuiaResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Abertura concorrente: duas aberturas ao mesmo tempo geram uma unica linha de historico.
 * Grava de verdade no H2 (sem @Transactional no teste); o @AfterEach apaga tudo o que foi criado.
 */
@SpringBootTest
@ActiveProfiles("test")
class AberturaPreGuiaConcorrenciaTest {

    private static final String SETOR = "setor.concorrencia";
    private static final String BENEFICIARIO = "70000000201";

    @Autowired private AberturaPreGuiaService service;
    @Autowired private SimulacaoLogin simulacaoLogin;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private PreGuiaHistoricoRepository historicoRepository;

    private Long idPreGuia;
    private Long idBeneficiario;
    private Long idOcs;

    @BeforeEach
    void dados() {
        usuario(SETOR, PerfilUsuario.SETOR_GUIAS);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Beneficiario Concorrencia");
        beneficiario.setPrecCp("70000000222");
        beneficiario.setDataNascimento(LocalDate.of(1968, 1, 1));
        beneficiario.setTelefone("12999990000");
        idBeneficiario = beneficiarioRepository.saveAndFlush(beneficiario).getUsuarioId();

        Ocs ocs = new Ocs();
        ocs.setNome("OCS Concorrencia Abertura");
        ocs.setContratoNum("CONC-ABERTURA");
        ocs.setTipo(OcsTipo.OCS);
        idOcs = ocsRepository.saveAndFlush(ocs).getIdOcs();

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(PreGuiaStatus.PENDENTE);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        idPreGuia = preGuiaRepository.saveAndFlush(preGuia).getIdPreGuia();
    }

    @AfterEach
    void limpar() {
        historicoRepository.deleteAll(historicoRepository.findAll().stream()
                .filter(h -> h.getPreGuia().getIdPreGuia().equals(idPreGuia)).toList());
        preGuiaRepository.deleteById(idPreGuia);
        ocsRepository.deleteById(idOcs);
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

    @Test
    void duasAberturasSimultaneas_geramUmaUnicaLinhaDeHistorico() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<AberturaPreGuiaResponse>> aberturas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                aberturas.add(executor.submit(() -> {
                    largada.await();
                    simulacaoLogin.definir(SETOR); // o login fica no ThreadLocal de cada thread
                    try {
                        return service.abrir(idPreGuia);
                    } finally {
                        simulacaoLogin.limpar();
                    }
                }));
            }
            largada.countDown(); // solta as duas threads ao mesmo tempo

            for (Future<AberturaPreGuiaResponse> abertura : aberturas) {
                assertThat(abertura.get(15, TimeUnit.SECONDS).status()).isEqualTo("EM_ANALISE");
            }

            long linhas = historicoRepository.findAll().stream()
                    .filter(h -> h.getPreGuia().getIdPreGuia().equals(idPreGuia))
                    .count();
            assertThat(linhas).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
