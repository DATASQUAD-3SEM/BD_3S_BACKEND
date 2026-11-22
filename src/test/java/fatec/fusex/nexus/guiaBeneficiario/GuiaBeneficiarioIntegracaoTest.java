package fatec.fusex.nexus.guiaBeneficiario;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * T18 (SCRUM-167): Minhas guias sem transacao de teste envolvendo a requisicao, como na aplicacao real (open-in-view
 * desligado). Grava no H2 de verdade; o @AfterEach apaga tudo o que o teste criou.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuiaBeneficiarioIntegracaoTest {

    private static final String LOGIN_TITULAR = "73100000001";
    private static final String LOGIN_SETOR = "setor.guias.integracao";
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BeneficiarioRepository beneficiarioRepository;
    @Autowired
    private OcsRepository ocsRepository;
    @Autowired
    private PreGuiaRepository preGuiaRepository;
    @Autowired
    private GuiaRepository guiaRepository;
    @Autowired
    private GuiaProcedimentoRepository procedimentoRepository;
    @Autowired
    private SessaoRepository sessaoRepository;

    private Beneficiario titular;
    private Usuario setor;
    private Ocs ocs;
    private Guia guiaConcluida;
    private Guia guiaSemAssinaturas;

    @BeforeEach
    void dados() {
        Usuario usuarioTitular = usuario(LOGIN_TITULAR, PerfilUsuario.BENEFICIARIO);
        titular = new Beneficiario();
        titular.setUsuario(usuarioTitular);
        titular.setNome("Titular Minhas Guias");
        titular.setPrecCp("73100000022");
        titular.setDataNascimento(LocalDate.of(1970, 1, 1));
        titular.setTelefone("12999990003");
        titular = beneficiarioRepository.saveAndFlush(titular);

        setor = usuario(LOGIN_SETOR, PerfilUsuario.SETOR_GUIAS);

        ocs = new Ocs();
        ocs.setNome("OCS Minhas Guias");
        ocs.setContratoNum("MG-001");
        ocs.setTipo(OcsTipo.OCS);
        ocs = ocsRepository.saveAndFlush(ocs);

        guiaConcluida = guia("731000001", PreGuiaStatus.CONCLUIDA, LocalDateTime.of(2026, 10, 1, 10, 0));
        guiaSemAssinaturas = guia("731000002", PreGuiaStatus.EM_ANALISE, LocalDateTime.of(2026, 10, 20, 10, 0));
    }

    @AfterEach
    void limpar() {
        for (Guia guia : new Guia[] {guiaConcluida, guiaSemAssinaturas}) {
            procedimentoRepository.findByGuiaIdGuiaInOrderByIdGuiaProcedimentoAsc(java.util.List.of(guia.getIdGuia()))
                    .forEach(p -> {
                        sessaoRepository.findAll().stream()
                                .filter(s -> s.getGuiaProcedimento().getIdGuiaProcedimento()
                                        .equals(p.getIdGuiaProcedimento()))
                                .forEach(sessaoRepository::delete);
                        procedimentoRepository.delete(p);
                    });
            guiaRepository.deleteById(guia.getIdGuia());
            preGuiaRepository.deleteById(guia.getPreGuia().getIdPreGuia());
        }
        ocsRepository.deleteById(ocs.getIdOcs());
        beneficiarioRepository.deleteById(titular.getUsuarioId());
        usuarioRepository.findByLogin(LOGIN_TITULAR).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByLogin(LOGIN_SETOR).ifPresent(usuarioRepository::delete);
    }

    @Test
    void lista_soAsGuiasDePreGuiaConcluida() throws Exception {
        mockMvc.perform(get("/guias/minhas").header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(guiaConcluida.getIdGuia()))
                .andExpect(jsonPath("$[0].ocs").value("OCS Minhas Guias"))
                .andExpect(jsonPath("$[0].procedimentos[0].quantidade").value(2))
                .andExpect(jsonPath("$[0].paciente.nome").value("Titular Minhas Guias"));
    }

    private Guia guia(String numero, PreGuiaStatus statusPreGuia, LocalDateTime insercao) {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(statusPreGuia);
        preGuia.setBeneficiario(titular);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamento-" + numero + ".pdf");
        preGuia.setDataCriacao(insercao.minusDays(1));
        preGuia = preGuiaRepository.saveAndFlush(preGuia);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(numero);
        guia.setArquivoUrl("guia-" + numero + ".pdf");
        guia.setDataInsercao(insercao);
        guia.setUsuario(setor);
        guia.setDataValidadeAte(FUTURA);
        guia.setSituacaoExecucao(SituacaoExecucao.NAO_ENVIADA);
        guia = guiaRepository.saveAndFlush(guia);

        GuiaProcedimento procedimento = new GuiaProcedimento();
        procedimento.setGuia(guia);
        procedimento.setCodDgp("40304361");
        procedimento.setDescricaoProcedimento("Hemograma completo");
        procedimento.setQuantidadeAutorizada(2);
        procedimentoRepository.saveAndFlush(procedimento);
        return guia;
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
