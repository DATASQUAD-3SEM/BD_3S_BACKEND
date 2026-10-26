package fatec.fusex.nexus.preGuia;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.dto.CriarPreGuiaMedicoRequest;
import fatec.fusex.nexus.preGuia.dto.PreGuiaMedicoResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRolAns;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Grava de verdade no H2 (sem @Transactional no teste), para cobrir o que o Mockito nao cobre:
 * os procedimentos e o historico persistidos e dois envios simultaneos com numeros diferentes.
 * O @AfterEach apaga tudo o que o teste criou, para nao atrapalhar os outros testes.
 */
@SpringBootTest
@ActiveProfiles("test")
class MedicoPreGuiaServiceIntegracaoTest {

    private static final String LOGIN_MEDICO = "70000000001";

    private final MedicoPreGuiaService service;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final ProcedimentoExameRepository procedimentoRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    private Long beneficiarioId;
    private Long ocsId;

    MedicoPreGuiaServiceIntegracaoTest(
            MedicoPreGuiaService service,
            UsuarioRepository usuarioRepository,
            MedicoFusexRepository medicoRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            ProcedimentoExameRepository procedimentoRepository,
            PreGuiaRepository preGuiaRepository,
            PreGuiaProcedimentoRepository preGuiaProcedimentoRepository,
            PreGuiaHistoricoRepository historicoRepository) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.preGuiaProcedimentoRepository = preGuiaProcedimentoRepository;
        this.historicoRepository = historicoRepository;
    }

    @BeforeEach
    void dados() {
        Usuario usuarioMedico = usuario(LOGIN_MEDICO, PerfilUsuario.MEDICO);
        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuarioMedico);
        medico.setNome("Medico Integracao");
        medico.setPatente("Capitao");
        medico.setCrm("CRM-INTEG-1");
        medico.setPrecCp("70000000011");
        medicoRepository.saveAndFlush(medico);

        Usuario usuarioBenef = usuario("70000000002", PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioBenef);
        beneficiario.setNome("Beneficiario Integracao");
        beneficiario.setPrecCp("70000000022");
        beneficiario.setDataNascimento(LocalDate.of(1968, 1, 1));
        beneficiario.setTelefone("12999990000");
        beneficiarioId = beneficiarioRepository.saveAndFlush(beneficiario).getUsuarioId();

        Ocs ocs = new Ocs();
        ocs.setNome("OCS Integracao");
        ocs.setContratoNum("INTEG-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsId = ocsRepository.saveAndFlush(ocs).getIdOcs();

        procedimento("70000001", "Procedimento Integracao A");
        procedimento("70000002", "Procedimento Integracao B");
    }

    @AfterEach
    void limpar() {
        historicoRepository.deleteAll();
        preGuiaProcedimentoRepository.deleteAll();
        preGuiaRepository.deleteAll();
        procedimentoRepository.deleteAll(procedimentoRepository.findByCodigoTussIn(List.of("70000001", "70000002")));
        ocsRepository.deleteById(ocsId);
        beneficiarioRepository.deleteById(beneficiarioId);
        medicoRepository.findByUsuarioLogin(LOGIN_MEDICO).ifPresent(medicoRepository::delete);
        usuarioRepository.findByLogin(LOGIN_MEDICO).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin("70000000002").ifPresent(usuarioRepository::delete);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void procedimento(String codigoTuss, String nome) {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss(codigoTuss);
        p.setTerminologiaProcedimento(nome);
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        procedimentoRepository.saveAndFlush(p);
    }

    private CriarPreGuiaMedicoRequest request() {
        return new CriarPreGuiaMedicoRequest(beneficiarioId, ocsId, List.of("70000001", "70000002"));
    }

    @Test
    void criar_gravaPreGuiaComProcedimentosEPrimeiraLinhaDoHistorico() {
        PreGuiaMedicoResponse resposta = service.criar(LOGIN_MEDICO, request());

        assertThat(resposta.numero()).isEqualTo(String.format("%04d", resposta.id()));
        assertThat(resposta.status()).isEqualTo("PENDENTE");

        assertThat(preGuiaProcedimentoRepository.findByPreGuiaIdPreGuia(resposta.id())).hasSize(2);
        assertThat(historicoRepository.findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(
                resposta.id(), PreGuiaStatus.PENDENTE)).isPresent();
    }

    @Test
    void criar_doisEnviosSimultaneos_geramNumerosDiferentes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<PreGuiaMedicoResponse>> envios = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                envios.add(executor.submit(() -> {
                    largada.await();
                    return service.criar(LOGIN_MEDICO, request());
                }));
            }
            largada.countDown(); // solta as duas threads ao mesmo tempo

            String numero1 = envios.get(0).get(10, TimeUnit.SECONDS).numero();
            String numero2 = envios.get(1).get(10, TimeUnit.SECONDS).numero();

            assertThat(numero1).isNotEqualTo(numero2);
            assertThat(numero1).hasSizeGreaterThanOrEqualTo(4);
            assertThat(numero2).hasSizeGreaterThanOrEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }
}
