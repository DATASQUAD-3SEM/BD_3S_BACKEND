package fatec.fusex.nexus.chefeBeneficiario;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeBeneficiarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        Usuario chefe = new Usuario();
        chefe.setLogin("chefe.teste");
        chefe.setSenhaHash("hash");
        chefe.setPerfil(PerfilUsuario.CHEFE_FUSEX);
        chefe.setAtivo(true);
        usuarioRepository.save(chefe);

        criarBeneficiario("11111111111", "João Silva", "22222222222", true);
        criarBeneficiario("33333333333", "Maria Souza", "44444444444", false);
    }

    private void criarBeneficiario(String cpf, String nome, String precCp, boolean ativo) {
        Usuario u = new Usuario();
        u.setLogin(cpf); u.setSenhaHash("hash"); u.setPerfil(PerfilUsuario.BENEFICIARIO); u.setAtivo(ativo);
        u = usuarioRepository.save(u);

        Beneficiario b = new Beneficiario();
        b.setUsuario(u); b.setNome(nome); b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1990, 1, 1)); b.setTelefone("11999999999");
        beneficiarioRepository.save(b);
    }

    @Test
    @DisplayName("Listar: padrão ativos")
    void listar_padraoAtivos() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("João Silva"));
    }

    @Test
    @DisplayName("Listar: filtro inativos")
    void listar_inativos() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?situacao=inativos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Maria Souza"));
    }

    @Test
    @DisplayName("Listar: busca por nome sem acento")
    void listar_buscaNomeSemAcento() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?busca=Joao&situacao=todos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("João Silva"));
    }

    @Test
    @DisplayName("Listar: busca por CPF")
    void listar_buscaCpf() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?busca=33333333333&situacao=todos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Maria Souza"));
    }

    @Test
    @DisplayName("Acesso negado para perfil errado (403)")
    void perfilErrado_retorna403() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
    }
}