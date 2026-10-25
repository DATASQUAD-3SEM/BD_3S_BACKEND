package fatec.fusex.nexus.correcaoMedico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimento;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoId;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRolAns;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;

/** Ponta a ponta (filtro de login + controller + service + H2) do PUT /medicos/me/pre-guias/{id}/correcao. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class SalvarCorrecaoControllerTest {

    private static final String MEDICO = "11144477735";
    private static final String OUTRO_MEDICO = "39053344705";
    private static final String BENEFICIARIO = "52998224725";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MedicoFusexRepository medicoRepository;
    @Autowired private BeneficiarioRepository beneficiarioRepository;
    @Autowired private OcsRepository ocsRepository;
    @Autowired private ProcedimentoExameRepository procedimentoRepository;
    @Autowired private PreGuiaRepository preGuiaRepository;
    @Autowired private PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    @Autowired private PreGuiaHistoricoRepository historicoRepository;
    @Autowired private EntityManager entityManager;

    private Usuario setorGuias;
    private MedicoFusex medico;
    private Beneficiario beneficiario;
    private Ocs ocsAtual;
    private Ocs ocsNova;
    private Ocs ocsInativa;

    private Long idEmAtencao;
    private Long idPendente;
    private Long idDeOutroMedico;
    private Long idDoBeneficiario;

    @BeforeEach
    void dados() {
        setorGuias = usuario("setor.teste", PerfilUsuario.SETOR_GUIAS);
        medico = medico(MEDICO, "Carlos Eduardo Lima", "11122233344", "123456-SP");
        MedicoFusex outroMedico = medico(OUTRO_MEDICO, "Marta Souza Lima", "45810020001", "654321-RJ");

        beneficiario = new Beneficiario();
        beneficiario.setUsuario(usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO));
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("12987654321");
        beneficiarioRepository.saveAndFlush(beneficiario);

        ocsAtual = ocs("Laboratorio Exemplo", "EXEMPLO-001", true);
        ocsNova = ocs("Hospital Exemplo", "EXEMPLO-002", true);
        ocsInativa = ocs("Clinica Inativa", "EXEMPLO-003", false);

        ProcedimentoExame hemograma = procedimento("40304361", "Hemograma");
        procedimento("40302040", "Glicose");
        procedimento("40301630", "Colesterol total");

        idEmAtencao = preGuia(medico, PreGuiaStatus.ATENCAO);
        vincular(idEmAtencao, hemograma);
        historico(idEmAtencao, PreGuiaStatus.ATENCAO, "Exame errado, troque o hemograma", setorGuias);

        idPendente = preGuia(medico, PreGuiaStatus.PENDENTE);
        idDeOutroMedico = preGuia(outroMedico, PreGuiaStatus.ATENCAO);
        idDoBeneficiario = preGuia(null, PreGuiaStatus.ATENCAO);
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

    private Ocs ocs(String nome, String contrato, boolean ativo) {
        Ocs o = new Ocs();
        o.setNome(nome);
        o.setContratoNum(contrato);
        o.setTipo(OcsTipo.OCS);
        o.setAtivo(ativo);
        return ocsRepository.saveAndFlush(o);
    }

    private ProcedimentoExame procedimento(String codigoTuss, String nome) {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss(codigoTuss);
        p.setTerminologiaProcedimento(nome);
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        return procedimentoRepository.saveAndFlush(p);
    }

    private Long preGuia(MedicoFusex criador, PreGuiaStatus status) {
        PreGuia p = new PreGuia();
        p.setBeneficiario(beneficiario);
        p.setOcs(ocsAtual);
        p.setMedicoFusex(criador);
        p.setStatus(status);
        if (criador == null) {
            p.setEncaminhamentoUrl("encaminhamentos/teste.pdf"); // rota do beneficiario
        }
        return preGuiaRepository.saveAndFlush(p).getIdPreGuia();
    }

    private void vincular(Long idPreGuia, ProcedimentoExame procedimento) {
        preGuiaProcedimentoRepository.saveAndFlush(new PreGuiaProcedimento(
                new PreGuiaProcedimentoId(idPreGuia, procedimento.getIdProcedimentoExame()),
                preGuiaRepository.getReferenceById(idPreGuia), procedimento));
    }

    private void historico(Long idPreGuia, PreGuiaStatus status, String observacao, Usuario autor) {
        historicoRepository.saveAndFlush(new PreGuiaHistorico(
                null, preGuiaRepository.getReferenceById(idPreGuia), status,
                observacao, null, autor, LocalDateTime.of(2026, 10, 15, 9, 0)));
    }

    private ResultActions salvar(String login, Long id, String corpo) throws Exception {
        return mockMvc.perform(put("/medicos/me/pre-guias/" + id + "/correcao")
                .header(SimulacaoLoginFilter.HEADER, login)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private void recarregar() {
        entityManager.flush();
        entityManager.clear();
    }

    private PreGuia preGuiaNoBanco(Long id) {
        recarregar();
        return preGuiaRepository.findById(id).orElseThrow();
    }

    private List<String> codigosNoBanco(Long id) {
        recarregar();
        return preGuiaProcedimentoRepository.findByPreGuiaIdPreGuia(id).stream()
                .map(pp -> pp.getProcedimentoExame().getCodigoTuss())
                .sorted()
                .toList();
    }

    // ===== sucesso =====

    @Test
    void soExames_substituiAListaVoltaParaPendenteEMantemAOcs() throws Exception {
        salvar(MEDICO, idEmAtencao, "{\"procedimentos\":[\"40302040\",\"40301630\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataReenvio").exists());

        assertThat(codigosNoBanco(idEmAtencao)).containsExactly("40301630", "40302040");
        PreGuia preGuia = preGuiaNoBanco(idEmAtencao);
        assertThat(preGuia.getStatus()).isEqualTo(PreGuiaStatus.PENDENTE);
        assertThat(preGuia.getOcs().getIdOcs()).isEqualTo(ocsAtual.getIdOcs());
        // o beneficiario nao muda
        assertThat(preGuia.getBeneficiario().getUsuarioId()).isEqualTo(beneficiario.getUsuarioId());
    }

    @Test
    void soOcs_trocaAOcsEMantemOsExames() throws Exception {
        salvar(MEDICO, idEmAtencao, "{\"ocsId\":" + ocsNova.getIdOcs() + ",\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isOk());

        assertThat(preGuiaNoBanco(idEmAtencao).getOcs().getIdOcs()).isEqualTo(ocsNova.getIdOcs());
        assertThat(codigosNoBanco(idEmAtencao)).containsExactly("40304361");
    }

    @Test
    void osDois_trocaOcsEExames() throws Exception {
        salvar(MEDICO, idEmAtencao,
                "{\"ocsId\":" + ocsNova.getIdOcs() + ",\"procedimentos\":[\"40304361\",\"40302040\"]}")
                .andExpect(status().isOk());

        assertThat(preGuiaNoBanco(idEmAtencao).getOcs().getIdOcs()).isEqualTo(ocsNova.getIdOcs());
        assertThat(codigosNoBanco(idEmAtencao)).containsExactly("40302040", "40304361");
    }

    @Test
    void historico_registraAcaoObservacaoDoSetorEOMedicoQueCorrigiu() throws Exception {
        salvar(MEDICO, idEmAtencao,
                "{\"ocsId\":" + ocsNova.getIdOcs() + ",\"procedimentos\":[\"40302040\"]}")
                .andExpect(status().isOk());

        recarregar();
        PreGuiaHistorico linha = historicoRepository
                .findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(idEmAtencao, PreGuiaStatus.PENDENTE)
                .orElseThrow();
        assertThat(linha.getUsuario().getIdUsuario()).isEqualTo(medico.getUsuarioId());
        assertThat(linha.getObservacao())
                .contains("exames e OCS alterados")
                .contains("Exame errado, troque o hemograma");
        assertThat(linha.getDataHora()).isNotNull();
    }

    // ===== 422 =====

    @Test
    void listaVazia_retorna422EmProcedimentos() throws Exception {
        salvar(MEDICO, idEmAtencao, "{\"procedimentos\":[]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(startsWith("procedimentos:")));
        salvar(MEDICO, idEmAtencao, "{}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(preGuiaNoBanco(idEmAtencao).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
    }

    @Test
    void codigoInexistente_retorna422CitandoOCodigo() throws Exception {
        salvar(MEDICO, idEmAtencao, "{\"procedimentos\":[\"40304361\",\"99999999\"]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(
                        "procedimentos: Codigo TUSS inexistente no catalogo: 99999999"));
        assertThat(codigosNoBanco(idEmAtencao)).containsExactly("40304361");
    }

    @Test
    void procedimentoRepetido_retorna422() throws Exception {
        salvar(MEDICO, idEmAtencao, "{\"procedimentos\":[\"40302040\",\" 40302040\"]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("procedimentos: Procedimento repetido: 40302040"));
    }

    @Test
    void ocsInativaOuInexistente_retorna422EmOcsId() throws Exception {
        salvar(MEDICO, idEmAtencao, "{\"ocsId\":" + ocsInativa.getIdOcs() + ",\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("ocsId: OCS inexistente ou inativa"));
        salvar(MEDICO, idEmAtencao, "{\"ocsId\":999999,\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isUnprocessableEntity());
        assertThat(preGuiaNoBanco(idEmAtencao).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
    }

    // ===== 409, 404 e 403 =====

    @Test
    void statusDiferenteDeAtencao_retorna409() throws Exception {
        salvar(MEDICO, idPendente, "{\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isConflict());
    }

    @Test
    void salvarDuasVezes_aSegundaRetorna409() throws Exception {
        salvar(MEDICO, idEmAtencao, "{\"procedimentos\":[\"40302040\"]}").andExpect(status().isOk());
        salvar(MEDICO, idEmAtencao, "{\"procedimentos\":[\"40301630\"]}").andExpect(status().isConflict());

        assertThat(codigosNoBanco(idEmAtencao)).containsExactly("40302040");
    }

    @Test
    void deOutroMedico_retorna404() throws Exception {
        salvar(MEDICO, idDeOutroMedico, "{\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isNotFound());
    }

    @Test
    void inexistente_retorna404() throws Exception {
        salvar(MEDICO, 999999L, "{\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isNotFound());
    }

    @Test
    void criadaPorBeneficiario_retorna403() throws Exception {
        salvar(MEDICO, idDoBeneficiario, "{\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isForbidden());
        assertThat(preGuiaNoBanco(idDoBeneficiario).getStatus()).isEqualTo(PreGuiaStatus.ATENCAO);
    }

    @Test
    void perfilErrado_retorna403() throws Exception {
        salvar(BENEFICIARIO, idEmAtencao, "{\"procedimentos\":[\"40304361\"]}")
                .andExpect(status().isForbidden());
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(put("/medicos/me/pre-guias/" + idEmAtencao + "/correcao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"procedimentos\":[\"40304361\"]}"))
                .andExpect(status().isUnauthorized());
    }
}
