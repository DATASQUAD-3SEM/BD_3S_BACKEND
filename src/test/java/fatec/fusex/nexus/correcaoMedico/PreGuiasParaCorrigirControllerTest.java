package fatec.fusex.nexus.correcaoMedico;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** Ponta a ponta (filtro de login + controller + service + H2) do GET /medicos/me/pre-guias/para-corrigir. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PreGuiasParaCorrigirControllerTest {

    private static final String URL = "/medicos/me/pre-guias/para-corrigir";
    private static final String MEDICO = "11144477735";
    private static final String OUTRO_MEDICO = "39053344705";
    private static final String MEDICO_SEM_PENDENCIA = "12345678909";
    private static final String BENEFICIARIO = "52998224725";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaHistoricoRepository historicoRepository;

    private Usuario setorGuias;
    private MedicoFusex medico;
    private MedicoFusex outroMedico;
    private Beneficiario beneficiario;
    private Ocs ocs;

    private Long idAntiga;
    private Long idRecente;

    PreGuiasParaCorrigirControllerTest(
            MockMvc mockMvc,
            UsuarioRepository usuarioRepository,
            MedicoFusexRepository medicoRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            PreGuiaRepository preGuiaRepository,
            PreGuiaHistoricoRepository historicoRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.historicoRepository = historicoRepository;
    }

    @BeforeEach
    void dados() {
        setorGuias = usuario("setor.teste", PerfilUsuario.SETOR_GUIAS);
        medico = medico(MEDICO, "Carlos Eduardo Lima", "11122233344", "123456-SP");
        outroMedico = medico(OUTRO_MEDICO, "Marta Souza Lima", "45810020001", "654321-RJ");
        medico(MEDICO_SEM_PENDENCIA, "Bruno Alves Costa", "45830040002", "777888-MG");

        beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);

        ocs = new Ocs();
        ocs.setNome("Laboratorio Exemplo");
        ocs.setContratoNum("EXEMPLO-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.saveAndFlush(ocs);

        // do medico logado, em ATENCAO: a mais recente e salva primeiro, para conferir a ordem
        idRecente = preGuia(medico, PreGuiaStatus.ATENCAO, LocalDateTime.of(2026, 10, 20, 9, 0));
        idAntiga = preGuia(medico, PreGuiaStatus.ATENCAO, LocalDateTime.of(2026, 10, 10, 9, 0));
        // a antiga teve duas devolucoes: vale a observacao mais recente
        historico(idAntiga, "Primeira observacao", LocalDateTime.of(2026, 10, 11, 9, 0));
        historico(idAntiga, "Observacao mais recente", LocalDateTime.of(2026, 10, 15, 9, 0));
        historico(idRecente, "Exame repetido", LocalDateTime.of(2026, 10, 21, 9, 0));

        // fora da lista: outro status, outro medico e criada pelo beneficiario
        preGuia(medico, PreGuiaStatus.PENDENTE, LocalDateTime.of(2026, 10, 1, 9, 0));
        preGuia(medico, PreGuiaStatus.EM_ANALISE, LocalDateTime.of(2026, 10, 2, 9, 0));
        preGuia(outroMedico, PreGuiaStatus.ATENCAO, LocalDateTime.of(2026, 10, 3, 9, 0));
        preGuia(null, PreGuiaStatus.ATENCAO, LocalDateTime.of(2026, 10, 4, 9, 0));
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private MedicoFusex medico(String cpf, String nome, String precCp, String crm) {
        MedicoFusex m = new MedicoFusex();
        m.setUsuario(usuario(cpf, PerfilUsuario.MEDICO));
        m.setNome(nome);
        m.setPatente("Capitao");
        m.setPrecCp(precCp);
        m.setCrm(crm);
        return medicoRepository.saveAndFlush(m);
    }

    private Long preGuia(MedicoFusex criador, PreGuiaStatus status, LocalDateTime dataCriacao) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(beneficiario);
        p.setOcs(ocs);
        p.setMedicoFusex(criador);
        p.setStatus(status);
        p.setDataCriacao(dataCriacao);
        if (criador == null) {
            p.setEncaminhamentoUrl("encaminhamentos/teste.pdf"); // rota do beneficiario
        }
        return preGuiaRepository.saveAndFlush(p).getIdPreGuia();
    }

    private void historico(Long idPreGuia, String observacao, LocalDateTime dataHora) {
        historicoRepository.saveAndFlush(new PreGuiaHistorico(
                null, preGuiaRepository.getReferenceById(idPreGuia), PreGuiaStatus.ATENCAO,
                observacao, null, setorGuias, dataHora));
    }

    @Test
    void listaSoAsDoMedicoLogadoEmAtencao_daMaisAntigaParaAMaisRecente() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, MEDICO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id").value(contains(idAntiga.intValue(), idRecente.intValue())));
    }

    @Test
    void devolveOsCamposDoContratoComAObservacaoMaisRecente() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, MEDICO))
                .andExpect(jsonPath("$[0].id").value(idAntiga))
                .andExpect(jsonPath("$[0].numero").value(String.format("%04d", idAntiga)))
                .andExpect(jsonPath("$[0].beneficiarioNome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$[0].precCp").value("45872213300"))
                .andExpect(jsonPath("$[0].ocsNome").value("Laboratorio Exemplo"))
                .andExpect(jsonPath("$[0].dataCriacao").value("2026-10-10T09:00:00"))
                .andExpect(jsonPath("$[0].observacao").value("Observacao mais recente"))
                .andExpect(jsonPath("$[1].observacao").value("Exame repetido"))
                .andExpect(jsonPath("$[0].telefone").doesNotExist());
    }

    @Test
    void preGuiaDeOutroMedico_ficaForaEApareceSoParaEle() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, OUTRO_MEDICO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void semPendencias_retorna200ComListaVazia() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, MEDICO_SEM_PENDENCIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void perfilErrado_retorna403() throws Exception {
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, BENEFICIARIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL).header(SimulacaoLoginFilter.HEADER, "setor.teste"))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
