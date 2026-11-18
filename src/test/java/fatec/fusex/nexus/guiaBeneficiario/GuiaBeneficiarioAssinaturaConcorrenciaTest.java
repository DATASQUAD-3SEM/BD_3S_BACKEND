package fatec.fusex.nexus.guiaBeneficiario;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinatura.AssinaturaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaBeneficiario.dto.GuiaBeneficiarioResponse;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T18-BE-3, criterio de aceite: "Duas requisicoes simultaneas para a mesma sessao: so uma assina".
 * Duas threads assinam a mesma sessao SOLICITADA ao mesmo tempo: uma assina (sucesso) e a outra recebe 409.
 * Grava de verdade no H2 (sem @Transactional no teste); o @AfterEach apaga tudo o que foi criado.
 */
@SpringBootTest
@ActiveProfiles("test")
class GuiaBeneficiarioAssinaturaConcorrenciaTest {

    private static final String BENEFICIARIO = "77777777777";
    private static final String SETOR = "setor.assinar.concorrencia";
    private static final String MSG_SESSAO_NAO_SOLICITADA = "A sessao nao esta aguardando assinatura";

    private final GuiaBeneficiarioService service;
    private final SimulacaoLogin simulacaoLogin;
    private final PlatformTransactionManager transactionManager;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;

    private Long idUsuario;
    private Long idBeneficiario;
    private Long idAssinatura;
    private Long idOcs;
    private Long idPreGuia;
    private Long idGuia;
    private Long idSessao;

    GuiaBeneficiarioAssinaturaConcorrenciaTest(GuiaBeneficiarioService service,
                                               SimulacaoLogin simulacaoLogin,
                                               PlatformTransactionManager transactionManager,
                                               UsuarioRepository usuarioRepository,
                                               BeneficiarioRepository beneficiarioRepository,
                                               AssinaturaRepository assinaturaRepository,
                                               OcsRepository ocsRepository,
                                               PreGuiaRepository preGuiaRepository,
                                               GuiaRepository guiaRepository,
                                               GuiaProcedimentoRepository procedimentoRepository,
                                               SessaoRepository sessaoRepository) {
        this.service = service;
        this.simulacaoLogin = simulacaoLogin;
        this.transactionManager = transactionManager;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
    }

    @BeforeEach
    void dados() {
        Usuario setor = usuario(SETOR, PerfilUsuario.SETOR_GUIAS);
        Usuario usuario = usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
        idUsuario = usuario.getIdUsuario();

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario);
        beneficiario.setNome("Beneficiario Assinar Concorrencia");
        beneficiario.setPrecCp("77777777722");
        beneficiario.setDataNascimento(LocalDate.of(1968, 1, 1));
        beneficiario.setTelefone("12999990000");
        idBeneficiario = beneficiarioRepository.saveAndFlush(beneficiario).getUsuarioId();

        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(usuario);
        assinatura.setAssinaturaUrl("assinaturas/concorrencia.png");
        Assinatura assinaturaSalva = assinaturaRepository.saveAndFlush(assinatura);
        idAssinatura = assinaturaSalva.getIdAssinatura();

        // O exigirPerfil le Usuario.assinatura: sem o vinculo, o assinar responde 422.
        usuario.setAssinatura(assinaturaSalva);
        usuarioRepository.saveAndFlush(usuario);

        Ocs ocs = new Ocs();
        ocs.setNome("OCS Assinar Concorrencia");
        ocs.setContratoNum("CONC-ASSINAR");
        ocs.setTipo(OcsTipo.OCS);
        idOcs = ocsRepository.saveAndFlush(ocs).getIdOcs();

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setEncaminhamentoUrl("encaminhamentos/concorrencia-assinar.pdf");
        PreGuia preGuiaSalva = preGuiaRepository.saveAndFlush(preGuia);
        idPreGuia = preGuiaSalva.getIdPreGuia();

        Guia guia = new Guia();
        guia.setPreGuia(preGuiaSalva);
        guia.setNumero("8800000331");
        guia.setArquivoUrl("guia-concorrencia-assinar.pdf");
        guia.setDataInsercao(LocalDateTime.of(2026, 11, 10, 10, 0));
        guia.setUsuario(setor);
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
        guia.setSituacaoExecucao(SituacaoExecucao.EM_EXECUCAO);
        Guia guiaSalva = guiaRepository.saveAndFlush(guia);
        idGuia = guiaSalva.getIdGuia();

        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guiaSalva);
        procedimento.setCodDgp("41001109");
        procedimento.setDescricaoProcedimento("TC - Abdome superior");
        procedimento.setQuantidadeAutorizada(1);
        GuiaProcedimento procedimentoSalvo = procedimentoRepository.saveAndFlush(procedimento);

        Sessao sessao = new Sessao();
        sessao.setGuiaProcedimento(procedimentoSalvo);
        sessao.setNumero(1);
        sessao.setStatus(SituacaoSessao.SOLICITADA);
        sessao.setDataSolicitacao(LocalDateTime.of(2026, 11, 10, 9, 0));
        idSessao = sessaoRepository.saveAndFlush(sessao).getIdSessao();
    }

    @Test
    void duasAssinaturasSimultaneasDaMesmaSessao_soUmaAssina() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<GuiaBeneficiarioResponse>> cliques = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                cliques.add(executor.submit(() -> {
                    largada.await(); // solta as duas threads ao mesmo tempo
                    simulacaoLogin.definir(BENEFICIARIO); // o login fica no ThreadLocal de cada thread
                    try {
                        return service.assinar(idGuia, idSessao);
                    } finally {
                        simulacaoLogin.limpar();
                    }
                }));
            }
            largada.countDown();

            int assinadas = 0;
            int conflitos = 0;
            for (Future<GuiaBeneficiarioResponse> clique : cliques) {
                try {
                    clique.get(15, TimeUnit.SECONDS);
                    assinadas++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ConflitoException.class);
                    assertThat(e.getCause().getMessage()).isEqualTo(MSG_SESSAO_NAO_SOLICITADA);
                    conflitos++;
                }
            }

            assertThat(assinadas).isEqualTo(1);
            assertThat(conflitos).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        // Leitura final em transacao propria: assinadoPor e assinatura sao LAZY.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Sessao atual = sessaoRepository.findById(idSessao).orElseThrow();
            assertThat(atual.getStatus()).isEqualTo(SituacaoSessao.ASSINADA);
            assertThat(atual.getDataAssinatura()).isNotNull();
            assertThat(atual.getAssinadoPor().getIdUsuario()).isEqualTo(idUsuario);
            assertThat(atual.getAssinatura().getIdAssinatura()).isEqualTo(idAssinatura);

            long assinadasNoBanco = sessaoRepository
                    .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                            List.of(idGuia))
                    .stream()
                    .filter(s -> s.getStatus() == SituacaoSessao.ASSINADA)
                    .count();
            assertThat(assinadasNoBanco).isEqualTo(1);

            Guia guiaAtual = guiaRepository.findById(idGuia).orElseThrow();
            assertThat(guiaAtual.getSituacaoExecucao()).isEqualTo(SituacaoExecucao.REALIZADA);
        });
    }

    // Ordem pelas chaves estrangeiras: sessoes, procedimentos, guia, pre-guia, assinatura (o usuario aponta
    // para ela, por isso o vinculo e zerado antes), beneficiario, ocs e, por fim, os usuarios.
    @AfterEach
    void limpar() {
        if (idGuia != null) {
            guiaRepository.findById(idGuia).ifPresent(guia -> {
                List<Sessao> sessoes = sessaoRepository
                        .findByGuiaProcedimentoGuiaIdGuiaInOrderByGuiaProcedimentoIdGuiaProcedimentoAscNumeroAsc(
                                List.of(guia.getIdGuia()));
                sessaoRepository.deleteAll(sessoes);
                procedimentoRepository.deleteAll(
                        procedimentoRepository.findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(
                                List.of(guia.getIdGuia())));
                guiaRepository.delete(guia);
            });
        }
        if (idPreGuia != null) {
            preGuiaRepository.deleteById(idPreGuia);
        }
        usuarioRepository.findByLogin(BENEFICIARIO).ifPresent(usuario -> {
            usuario.setAssinatura(null);
            usuarioRepository.saveAndFlush(usuario);
        });
        if (idAssinatura != null) {
            assinaturaRepository.deleteById(idAssinatura);
        }
        if (idBeneficiario != null) {
            beneficiarioRepository.deleteById(idBeneficiario);
        }
        if (idOcs != null) {
            ocsRepository.deleteById(idOcs);
        }
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
}
