package fatec.fusex.nexus.MinhasPreGuias;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class MinhasPreGuiasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PreGuiaRepository preGuiaRepository;

    @Autowired
    private BeneficiarioRepository beneficiarioRepository;

    @Autowired
    private OcsRepository ocsRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Beneficiario beneficioLogado;
    private Beneficiario outroBeneficiario;
    private Ocs ocsPadrao;

    @BeforeEach
    void setUp() {
        // Sem deleteAll(): @Transactional reverte cada teste
        // e o profile "test" usa H2 em memória, sem seed.

        // --- 1. USUÁRIO E BENEFICIÁRIO LOGADO (u1) ---
        Usuario u1 = new Usuario();
        u1.setLogin("11111111111");
        u1.setSenhaHash("senha123");
        u1.setPerfil(PerfilUsuario.BENEFICIARIO);
        u1.setAtivo(true);
        u1 = usuarioRepository.save(u1);

        beneficioLogado = new Beneficiario();
        beneficioLogado.setUsuario(u1);
        beneficioLogado.setNome("João Logado");
        beneficioLogado.setPrecCp("11111111111");
        beneficioLogado.setDataNascimento(LocalDate.of(1990, 1, 1));
        beneficioLogado.setTelefone("11999999991");
        beneficioLogado = beneficiarioRepository.save(beneficioLogado);

        // --- 2. OUTRO USUÁRIO E BENEFICIÁRIO (u2) ---
        Usuario u2 = new Usuario();
        u2.setLogin("22222222222");
        u2.setSenhaHash("senha123");
        u2.setPerfil(PerfilUsuario.BENEFICIARIO);
        u2.setAtivo(true);
        u2 = usuarioRepository.save(u2);

        outroBeneficiario = new Beneficiario();
        outroBeneficiario.setUsuario(u2);
        outroBeneficiario.setNome("Maria Outra");
        outroBeneficiario.setPrecCp("22222222222");
        outroBeneficiario.setDataNascimento(LocalDate.of(1985, 5, 20));
        outroBeneficiario.setTelefone("11999999992");
        outroBeneficiario = beneficiarioRepository.save(outroBeneficiario);

        // --- 3. OCS ---
        ocsPadrao = new Ocs();
        ocsPadrao.setNome("Hospital Central");
        ocsPadrao.setContratoNum("123456789");
        ocsPadrao.setTipo(OcsTipo.OCS);
        ocsPadrao = ocsRepository.save(ocsPadrao);

        // --- 4. PRÉ-GUIAS DO JOÃO ---
        PreGuia p1 = new PreGuia();
        p1.setBeneficiario(beneficioLogado);
        p1.setOcs(ocsPadrao);
        p1.setStatus(PreGuiaStatus.PENDENTE);
        p1.setDataCriacao(LocalDateTime.now().minusDays(5));
        p1.setEncaminhamentoUrl("http://exemplo.com/encaminhamento1.pdf");
        preGuiaRepository.save(p1);

        PreGuia p2 = new PreGuia();
        p2.setBeneficiario(beneficioLogado);
        p2.setOcs(ocsPadrao);
        p2.setStatus(PreGuiaStatus.ATENCAO);
        p2.setDataCriacao(LocalDateTime.now().minusDays(2));
        p2.setEncaminhamentoUrl("http://exemplo.com/encaminhamento2.pdf");
        preGuiaRepository.save(p2);

        PreGuia p3 = new PreGuia();
        p3.setBeneficiario(beneficioLogado);
        p3.setOcs(ocsPadrao);
        p3.setStatus(PreGuiaStatus.CONCLUIDA);
        p3.setDataCriacao(LocalDateTime.now());
        p3.setEncaminhamentoUrl("http://exemplo.com/encaminhamento3.pdf");
        preGuiaRepository.save(p3);

        // --- 5. PRÉ-GUIA DA MARIA ---
        PreGuia pOutro = new PreGuia();
        pOutro.setBeneficiario(outroBeneficiario);
        pOutro.setOcs(ocsPadrao);
        pOutro.setStatus(PreGuiaStatus.PENDENTE);
        pOutro.setDataCriacao(LocalDateTime.now());
        pOutro.setEncaminhamentoUrl("http://exemplo.com/encaminhamento_maria.pdf");
        preGuiaRepository.save(pOutro);
    }

    @Test
    @DisplayName("Deve retornar 403 se o perfil for errado")
    void deveRetornar403ParaPerfilErrado() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .with(user("11111111111").roles("SETOR_GUIAS")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lista vazia deve retornar 200 e array vazio")
    void deveRetornarListaVazia() throws Exception {
        Usuario u3 = new Usuario();
        u3.setLogin("33333333333");
        u3.setSenhaHash("senha123");
        u3.setPerfil(PerfilUsuario.BENEFICIARIO);
        u3.setAtivo(true);
        u3 = usuarioRepository.save(u3);

        Beneficiario semGuias = new Beneficiario();
        semGuias.setUsuario(u3);
        semGuias.setNome("Sem Guias");
        semGuias.setPrecCp("33333333333");
        semGuias.setDataNascimento(LocalDate.of(2000, 1, 1));
        semGuias.setTelefone("11999999993");
        beneficiarioRepository.save(semGuias);

        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .with(user("33333333333").roles("BENEFICIARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Deve listar apenas do beneficiário logado, ordenado, e filtrar campos por status")
    void deveListarMinhasPreGuiasComRegrasDeStatus() throws Exception {
        mockMvc.perform(get("/beneficiarios/me/pre-guias")
                        .with(user("11111111111").roles("BENEFICIARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status").value("CONCLUIDA"))
                .andExpect(jsonPath("$[1].status").value("ATENCAO"))
                .andExpect(jsonPath("$[2].status").value("PENDENTE"));
    }
}