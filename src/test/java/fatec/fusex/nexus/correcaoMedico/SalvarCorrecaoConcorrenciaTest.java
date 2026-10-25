package fatec.fusex.nexus.correcaoMedico;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.correcaoMedico.dto.SalvarCorrecaoRequest;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRolAns;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Envio duplo: dois reenvios ao mesmo tempo geram uma unica mudanca (um 200 e um 409).
 * Grava de verdade no H2 (sem @Transactional no teste); o @AfterEach apaga tudo o que foi criado.
 */
@SpringBootTest
@ActiveProfiles("test")
class SalvarCorrecaoConcorrenciaTest {

    private static final String MEDICO = "70000000101";

    private final SalvarCorrecaoService service;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final ProcedimentoExameRepository procedimentoRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    private Long idPreGuia;
    private Long idBeneficiario;
    private Long idOcs;

    SalvarCorrecaoConcorrenciaTest(
            SalvarCorrecaoService service,
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
        MedicoFusex medico = new MedicoFusex();
        medico.setUsuario(usuario(MEDICO, PerfilUsuario.MEDICO));
        medico.setNome("Medico Concorrencia");
        medico.setPatente("Capitao");
        medico.setPrecCp("70000000111");
        medico.setCrm("7000001-SP");
        medicoRepository.saveAndFlush(medico);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario("70000000102", PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Beneficiario Concorrencia");
        beneficiario.setPrecCp("70000000122");
        beneficiario.setDataNascimento(LocalDate.of(1968, 1, 1));
        beneficiario.setTelefone("12999990000");
        idBeneficiario = beneficiarioRepository.saveAndFlush(beneficiario).getUsuarioId();

        Ocs ocs = new Ocs();
        ocs.setNome("OCS Concorrencia");
        ocs.setContratoNum("CONC-001");
        ocs.setTipo(OcsTipo.OCS);
        idOcs = ocsRepository.saveAndFlush(ocs).getIdOcs();

        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss("70000101");
        p.setTerminologiaProcedimento("Procedimento Concorrencia");
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        procedimentoRepository.saveAndFlush(p);

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setMedicoFusex(medico);
        preGuia.setStatus(PreGuiaStatus.ATENCAO);
        idPreGuia = preGuiaRepository.saveAndFlush(preGuia).getIdPreGuia();
    }

    @AfterEach
    void limpar() {
        historicoRepository.deleteAll(historicoRepository.findAll().stream()
                .filter(h -> h.getPreGuia().getIdPreGuia().equals(idPreGuia)).toList());
        preGuiaProcedimentoRepository.deleteAll(preGuiaProcedimentoRepository.findByPreGuiaIdPreGuia(idPreGuia));
        preGuiaRepository.deleteById(idPreGuia);
        procedimentoRepository.deleteAll(procedimentoRepository.findByCodigoTuss("70000101"));
        ocsRepository.deleteById(idOcs);
        beneficiarioRepository.deleteById(idBeneficiario);
        medicoRepository.findByUsuarioLogin(MEDICO).ifPresent(medicoRepository::delete);
        usuarioRepository.findByLogin(MEDICO).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin("70000000102").ifPresent(usuarioRepository::delete);
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
    void doisReenviosSimultaneos_geramUmaUnicaMudanca() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        SalvarCorrecaoRequest request = new SalvarCorrecaoRequest(null, List.of("70000101"));
        try {
            List<Future<?>> envios = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                envios.add(executor.submit(() -> {
                    largada.await();
                    return service.salvar(idPreGuia, MEDICO, request);
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
            // so uma linha de historico do reenvio
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
