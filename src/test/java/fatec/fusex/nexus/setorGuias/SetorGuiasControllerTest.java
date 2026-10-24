package fatec.fusex.nexus.setorGuias;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class SetorGuiasControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Ocs ocsPadrao;

    private static final LocalDateTime D1 = LocalDateTime.of(2026, 10, 22, 10, 0);
    private static final LocalDateTime D2 = LocalDateTime.of(2026, 10, 21, 10, 0);
    private static final LocalDateTime D3 = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime D4 = LocalDateTime.of(2026, 10, 19, 10, 0);

    @BeforeEach
    void setUp() {
        Usuario setor = new Usuario();
        setor.setLogin("setor.guias");
        setor.setSenhaHash("hash");
        setor.setPerfil(PerfilUsuario.SETOR_GUIAS);
        setor.setAtivo(true);
        usuarioRepository.save(setor);

        ocsPadrao = new Ocs();
        ocsPadrao.setNome("Hospital Central");
        ocsPadrao.setContratoNum("CTR-001");
        ocsPadrao.setTipo(OcsTipo.OCS);
        ocsPadrao = ocsRepository.save(ocsPadrao);

        // Beneficiario com acento no nome
        Beneficiario jose = beneficiario("52998224725", "José da Silva", "45872213300");
        Beneficiario maria = beneficiario("11144477735", "Maria Souza", "11122233344");

        criarPreGuia(jose, PreGuiaStatus.PENDENTE, D1, null);
        criarPreGuia(jose, PreGuiaStatus.ATENCAO, D2, null);
        criarPreGuia(maria, PreGuiaStatus.EM_ANALISE, D3, null);
        criarPreGuia(maria, PreGuiaStatus.CONCLUIDA, D4, null);
    }

    private Beneficiario beneficiario(String login, String nome, String precCp) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(PerfilUsuario.BENEFICIARIO);
        u.setAtivo(true);
        u = usuarioRepository.save(u);

        Beneficiario b = new Beneficiario();
        b.setUsuario(u);
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1990, 1, 1));
        b.setTelefone("11987654321");
        return beneficiarioRepository.save(b);
    }

    private void criarPreGuia(Beneficiario b, PreGuiaStatus s, LocalDateTime data,
                              fatec.fusex.nexus.medicoFusex.MedicoFusex medico) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(b);
        p.setOcs(ocsPadrao);
        p.setStatus(s);
        p.setDataCriacao(data);
        p.setEncaminhamentoUrl("encaminhamentos/x.pdf");
        p.setMedicoFusex(medico);
        preGuiaRepository.save(p);
    }

    // ---------- Perfil ----------

    @Test
    @DisplayName("Sem header retorna 401")
    void semHeader_retorna401() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Perfil diferente de SETOR_GUIAS retorna 403")
    void perfilErrado_retorna403() throws Exception {
        Usuario medico = new Usuario();
        medico.setLogin("medico.teste");
        medico.setSenhaHash("hash");
        medico.setPerfil(PerfilUsuario.MEDICO);
        medico.setAtivo(true);
        usuarioRepository.save(medico);

        mockMvc.perform(get("/setor-guias/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "medico.teste"))
                .andExpect(status().isForbidden());
    }

    // ---------- Sem filtro ----------

    @Test
    @DisplayName("Sem filtro retorna tudo ordenado por dataCriacao DESC")
    void semFiltro_retornaTudoOrdenado() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.itens", hasSize(4)))
                .andExpect(jsonPath("$.itens[0].dataCriacao").value("2026-10-22T10:00:00"))
                .andExpect(jsonPath("$.itens[3].dataCriacao").value("2026-10-19T10:00:00"));
    }

    @Test
    @DisplayName("Sem filtro expoe os campos do contrato")
    void semFiltro_expoeCampos() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").isNumber())
                .andExpect(jsonPath("$.itens[0].numero").isString())
                .andExpect(jsonPath("$.itens[0].beneficiarioNome").isString())
                .andExpect(jsonPath("$.itens[0].ocsNome").value("Hospital Central"))
                .andExpect(jsonPath("$.itens[0].origem").value("BENEFICIARIO"))
                .andExpect(jsonPath("$.itens[0].status").value("PENDENTE"));
    }

    // ---------- Filtros de status ----------

    @Test
    @DisplayName("Filtra por Pendente")
    void filtroPendente() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("status", "Pendente")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].status").value("PENDENTE"));
    }

    @Test
    @DisplayName("Filtra por Em análise (com acento)")
    void filtroEmAnalise() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("status", "Em análise")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].status").value("EM_ANALISE"));
    }

    @Test
    @DisplayName("Filtra por Atenção (com acento)")
    void filtroAtencao() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("status", "Atenção")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].status").value("ATENCAO"));
    }

    @Test
    @DisplayName("Filtra por Concluída (com acento)")
    void filtroConcluida() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("status", "Concluída")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].status").value("CONCLUIDA"));
    }

    @Test
    @DisplayName("Status invalido retorna 400")
    void statusInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("status", "Rascunho")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Busca ----------

    @Test
    @DisplayName("Busca por nome (parcial) encontra Jose")
    void buscaPorNome_parcial() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("busca", "Jose")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("Busca com acento no input encontra nome sem acento")
    void buscaComAcento_noInput() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("busca", "José")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("Busca sem acento encontra nome com acento")
    void buscaSemAcento_noInput() throws Exception {
        // Nome no banco: "José da Silva"
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("busca", "jose")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("Busca por Prec CP")
    void buscaPorPrecCp() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("busca", "11122233344")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.itens[0].beneficiarioNome").value("Maria Souza"));
    }

    @Test
    @DisplayName("Busca por nome com minuscula encontra mesmo assim")
    void buscaCaseInsensitive() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("busca", "MARIA")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("Busca sem resultados retorna total 0 e itens vazio")
    void buscaSemResultado() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("busca", "ninguem")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    // ---------- Paginacao ----------

    @Test
    @DisplayName("Paginacao: page=0,size=2 retorna 2 itens de 4")
    void paginacao_primeiraPagina() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("page", "0")
                        .param("size", "2")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].dataCriacao").value("2026-10-22T10:00:00"));
    }

    @Test
    @DisplayName("Paginacao: page=1,size=2 retorna os 2 restantes")
    void paginacao_segundaPagina() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("page", "1")
                        .param("size", "2")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.itens", hasSize(2)));
    }

    @Test
    @DisplayName("Pagina vazia (page grande) retorna itens vazio, total mantido")
    void paginaVazia() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("page", "10")
                        .param("size", "20")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }

    @Test
    @DisplayName("size > 100 retorna 400")
    void sizeAcimaDoLimite_retorna400() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("size", "101")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("page negativa retorna 400")
    void pageNegativa_retorna400() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias")
                        .param("page", "-1")
                        .header(SimulacaoLoginFilter.HEADER, "setor.guias"))
                .andExpect(status().isBadRequest());
    }
}