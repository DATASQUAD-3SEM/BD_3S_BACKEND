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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChefeBeneficiarioControllerTest {

    private final MockMvc mockMvc;
    private final BeneficiarioRepository beneficiarioRepository;
    private final UsuarioRepository usuarioRepository;

    ChefeBeneficiarioControllerTest(
            MockMvc mockMvc,
            BeneficiarioRepository beneficiarioRepository,
            UsuarioRepository usuarioRepository) {
        this.mockMvc = mockMvc;
        this.beneficiarioRepository = beneficiarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

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

    // ===== situacao =====

    @Test
    @DisplayName("Listar: filtro todos traz ativos e inativos, com os campos do contrato")
    void listar_todos() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?situacao=todos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("João Silva", "Maria Souza")))
                .andExpect(jsonPath("$.itens[0].id").isNumber())
                .andExpect(jsonPath("$.itens[0].cpf").value("11111111111"))
                .andExpect(jsonPath("$.itens[0].precCp").value("22222222222"))
                .andExpect(jsonPath("$.itens[0].ativo").value(true))
                .andExpect(jsonPath("$.itens[1].ativo").value(false))
                .andExpect(jsonPath("$.itens[0].telefone").doesNotExist())
                .andExpect(jsonPath("$.itens[0].senhaHash").doesNotExist());
    }

    @Test
    @DisplayName("Listar: situacao vazia usa o padrao ativos")
    void listar_situacaoVazia_usaAtivos() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?situacao=").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("João Silva"));
    }

    @Test
    @DisplayName("Listar: situacao invalida retorna 400")
    void listar_situacaoInvalida_retorna400() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?situacao=excluidos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isBadRequest());
    }

    // ===== busca =====

    @Test
    @DisplayName("Listar: busca por Prec CP, com e sem mascara")
    void listar_buscaPrecCp() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?busca=44444444444&situacao=todos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Maria Souza"));
        mockMvc.perform(get("/chefe/beneficiarios?busca=444444444-44&situacao=todos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Maria Souza"));
    }

    @Test
    @DisplayName("Listar: busca por CPF com mascara")
    void listar_buscaCpfComMascara() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?busca=333.333.333-33&situacao=todos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("Maria Souza"));
    }

    @Test
    @DisplayName("Listar: busca respeita a situacao")
    void listar_buscaRespeitaSituacao() throws Exception {
        // Maria e inativa: nao aparece no padrao (ativos)
        mockMvc.perform(get("/chefe/beneficiarios?busca=maria").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("Listar: busca sem resultado e curinga do LIKE tratado como texto")
    void listar_buscaSemResultadoECuringa() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?busca=inexistente&situacao=todos").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.itens", hasSize(0)));
        mockMvc.perform(get("/chefe/beneficiarios").param("busca", "%").param("situacao", "todos")
                        .header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(jsonPath("$.total").value(0));
    }

    // ===== paginacao =====

    @Test
    @DisplayName("Listar: paginacao em ordem alfabetica, mantendo o total")
    void listar_paginacao() throws Exception {
        criarBeneficiario("55555555555", "Ana Pereira", "66666666666", true);

        mockMvc.perform(get("/chefe/beneficiarios?situacao=todos&page=0&size=2").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Ana Pereira", "João Silva")));

        mockMvc.perform(get("/chefe/beneficiarios?situacao=todos&page=1&size=2").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.itens[*].nome").value(contains("Maria Souza")));
    }

    @Test
    @DisplayName("Listar: page ou size invalido retorna 400")
    void listar_paginacaoInvalida_retorna400() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?page=-1").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/chefe/beneficiarios?size=0").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/chefe/beneficiarios?size=101").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isBadRequest());
    }

    // ===== acesso =====

    @Test
    @DisplayName("Perfil errado com situacao invalida: 403 antes da validacao")
    void perfilErradoComSituacaoInvalida_retorna403() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios?situacao=xyz").header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chefe inativo recebe 403")
    void chefeInativo_retorna403() throws Exception {
        Usuario chefe = usuarioRepository.findByLogin("chefe.teste").orElseThrow();
        chefe.setAtivo(false);
        usuarioRepository.saveAndFlush(chefe);

        mockMvc.perform(get("/chefe/beneficiarios").header(SimulacaoLoginFilter.HEADER, "chefe.teste"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sem login retorna 401")
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(get("/chefe/beneficiarios"))
                .andExpect(status().isUnauthorized());
    }
}