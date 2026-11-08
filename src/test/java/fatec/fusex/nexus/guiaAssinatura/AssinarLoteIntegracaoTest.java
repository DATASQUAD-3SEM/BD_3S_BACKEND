package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/**
 * Ponta a ponta do POST /guias/assinar-lote (T10-BE1) com o banco: filtro de login, servico do lote,
 * assinatura por guia e conclusao da pre-guia quando a segunda assinatura entra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AssinarLoteIntegracaoTest {

    private static final String CHEFE = "chefe.lote";
    private static final String MEDICO = "medico.lote";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaGuiaRepository assinaturaRepository;
    private final EntityManager em;

    private Usuario chefe;
    private Usuario medico;

    AssinarLoteIntegracaoTest(MockMvc mockMvc,
                              UsuarioRepository usuarioRepository,
                              AssinaturaGuiaRepository assinaturaRepository,
                              EntityManager em) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.em = em;
    }

    @BeforeEach
    void dados() {
        chefe = novoUsuario(CHEFE, PerfilUsuario.CHEFE_FUSEX);
        chefe.setAssinatura(novaAssinatura(chefe));
        medico = novoUsuario(MEDICO, PerfilUsuario.MEDICO);
        medico.setAssinatura(novaAssinatura(medico));
        em.flush();
    }

    @Test
    void loteDuasGuias_assinaAsDuasEConcluiSoAQueRecebeASegundaAssinatura() throws Exception {
        Guia semAssinatura = novaGuia("202610901");
        Guia comMedico = novaGuia("202610902");
        assine(comMedico, AssinaturaGuiaPapel.MEDICO, medico);
        em.flush();

        lote(CHEFE, "[" + semAssinatura.getIdGuia() + "," + comMedico.getIdGuia() + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assinadas.length()").value(2))
                .andExpect(jsonPath("$.falhas.length()").value(0));

        assertThat(statusDaPreGuia(semAssinatura)).isEqualTo(PreGuiaStatus.EM_ANALISE);
        assertThat(statusDaPreGuia(comMedico)).isEqualTo(PreGuiaStatus.CONCLUIDA);
    }

    @Test
    void guiaJaAssinadaPeloChefe_viraFalha207_eAsOutrasSaoAssinadas() throws Exception {
        Guia jaAssinada = novaGuia("202610903");
        Guia livre = novaGuia("202610904");
        assine(jaAssinada, AssinaturaGuiaPapel.CHEFE, chefe);
        em.flush();

        lote(CHEFE, "[" + jaAssinada.getIdGuia() + "," + livre.getIdGuia() + "]")
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.assinadas[0]").value(livre.getIdGuia()))
                .andExpect(jsonPath("$.falhas[0].guiaId").value(jaAssinada.getIdGuia()))
                .andExpect(jsonPath("$.falhas[0].motivo").value("ja_assinada_pelo_chefe"));

        assertThat(assinaturasDoChefe(livre)).isEqualTo(1);
    }

    @Test
    void guiaInexistente_viraFalha207ComMotivoNaoEncontrada() throws Exception {
        Guia existente = novaGuia("202610905");
        em.flush();

        lote(CHEFE, "[" + existente.getIdGuia() + ",987654321]")
                .andExpect(status().isMultiStatus())
                .andExpect(jsonPath("$.falhas[0].guiaId").value(987654321))
                .andExpect(jsonPath("$.falhas[0].motivo").value("guia_nao_encontrada"));
    }

    @Test
    void medicoNaoPodeUsarOLoteDoChefe_devolve403() throws Exception {
        Guia guia = novaGuia("202610906");
        em.flush();

        lote(MEDICO, "[" + guia.getIdGuia() + "]")
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions lote(String login, String guiaIds) throws Exception {
        return mockMvc.perform(post("/guias/assinar-lote")
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"guiaIds\":" + guiaIds + "}"));
    }

    private PreGuiaStatus statusDaPreGuia(Guia guia) {
        em.flush();
        em.clear();
        return em.find(Guia.class, guia.getIdGuia()).getPreGuia().getStatus();
    }

    private long assinaturasDoChefe(Guia guia) {
        em.flush();
        return assinaturaRepository.buscarComLockPorGuia(guia.getIdGuia()).stream()
                .filter(a -> a.getPapel() == AssinaturaGuiaPapel.CHEFE)
                .count();
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        em.persist(usuario);
        return usuario;
    }

    private void assine(Guia guia, AssinaturaGuiaPapel papel, Usuario usuario) {
        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setGuia(guia);
        assinatura.setPapel(papel);
        assinatura.setUsuario(usuario);
        assinatura.setAssinatura(usuario.getAssinatura());
        em.persist(assinatura);
    }

    private Assinatura novaAssinatura(Usuario usuario) {
        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(usuario);
        assinatura.setAssinaturaUrl("assinaturas/" + usuario.getLogin() + ".png");
        em.persist(assinatura);
        return assinatura;
    }

    private Guia novaGuia(String numero) {
        Ocs ocs = new Ocs();
        ocs.setNome("Ocs " + numero);
        ocs.setTipo(OcsTipo.OCS);
        ocs.setContratoNum("CT-" + numero);
        em.persist(ocs);

        Usuario usuarioBeneficiario = novoUsuario("benef-" + numero, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuarioBeneficiario);
        beneficiario.setNome("Beneficiario " + numero);
        beneficiario.setPrecCp("99" + numero);
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        em.persist(beneficiario);

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(PreGuiaStatus.EM_ANALISE);
        preGuia.setEncaminhamentoUrl("encaminhamentos/teste.pdf");
        em.persist(preGuia);

        Usuario inseridaPor = novoUsuario("setor-" + numero, PerfilUsuario.SETOR_GUIAS);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(numero);
        guia.setArquivoUrl("/arquivos/guia-" + numero + ".pdf");
        guia.setDataInsercao(LocalDateTime.now());
        guia.setUsuario(inseridaPor);
        em.persist(guia);
        return guia;
    }
}
