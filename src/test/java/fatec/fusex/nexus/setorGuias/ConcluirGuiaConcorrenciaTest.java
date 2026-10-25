package fatec.fusex.nexus.setorGuias;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
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
 * Conclusao concorrente: se dois cliques chegarem juntos, so um cria a Guia (um 201 e um 409).
 * Grava de verdade no H2 (sem @Transactional no teste); o @AfterEach apaga tudo o que foi criado.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcluirGuiaConcorrenciaTest {

    private static final String SETOR = "setor.concluir";
    private static final String BENEFICIARIO = "70000000301";

    @Autowired private ConcluirGuiaService service;
    @Autowired private SimulacaoLogin simulacaoLogin;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private GuiaRepository guiaRepository;
    @Autowired private GuiaRascunhoRepository guiaRascunhoRepository;

    private Long idPreGuia;
    private Long idBeneficiario;
    private Long idOcs;

    @BeforeEach
    void dados() {
        usuario(SETOR, PerfilUsuario.SETOR_GUIAS);

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Beneficiario Concluir");
        beneficiario.setPrecCp("70000000322");
        beneficiario.setDataNascimento(LocalDate.of(1968, 1, 1));
        beneficiario.setTelefone("12999990000");
        idBeneficiario = beneficiarioRepository.saveAndFlush(beneficiario).getUsuarioId();

        Ocs ocs = new Ocs();
        ocs.setNome("OCS Concluir");
        ocs.setContratoNum("CONC-CONCLUIR");
        ocs.setTipo(OcsTipo.OCS);
        idOcs = ocsRepository.saveAndFlush(ocs).getIdOcs();

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(PreGuiaStatus.EM_ANALISE);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        preGuia = preGuiaRepository.saveAndFlush(preGuia);
        idPreGuia = preGuia.getIdPreGuia();

        GuiaRascunho r = new GuiaRascunho();
        r.setPreGuia(preGuia);
        r.setNumero("7000000301");
        r.setArquivoUrl("guias-rascunho/concorrencia.pdf");
        r.setNomeArquivo("concorrencia.pdf");
        r.setDataAtualizacao(LocalDateTime.now());
        guiaRascunhoRepository.saveAndFlush(r);
    }

    @AfterEach
    void limpar() {
        guiaRepository.findByPreGuiaIdPreGuia(idPreGuia).ifPresent(guiaRepository::delete);
        guiaRascunhoRepository.deleteById(idPreGuia);
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
    void doisCliquesSimultaneos_soUmCriaAGuia() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<?>> cliques = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                cliques.add(executor.submit(() -> {
                    largada.await();
                    simulacaoLogin.definir(SETOR); // o login fica no ThreadLocal de cada thread
                    try {
                        return service.concluir(idPreGuia);
                    } finally {
                        simulacaoLogin.limpar();
                    }
                }));
            }
            largada.countDown(); // solta as duas threads ao mesmo tempo

            int criadas = 0;
            int conflitos = 0;
            for (Future<?> clique : cliques) {
                try {
                    clique.get(15, TimeUnit.SECONDS);
                    criadas++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ConflitoException.class);
                    conflitos++;
                }
            }

            assertThat(criadas).isEqualTo(1);
            assertThat(conflitos).isEqualTo(1);
            assertThat(guiaRepository.findByPreGuiaIdPreGuia(idPreGuia)).isPresent();
        } finally {
            executor.shutdownNow();
        }
    }
}
