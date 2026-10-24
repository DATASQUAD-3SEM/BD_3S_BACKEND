package fatec.fusex.nexus.correcaoMedico;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.beneficiario.FaixaEtaria;
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

/** Ponta a ponta (filtro de login + controller + service + H2) do GET /medicos/me/pre-guias/{id}/correcao. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CorrecaoMedicoControllerTest {

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

    private Usuario setorGuias;
    private MedicoFusex medico;
    private Beneficiario beneficiario;
    private Ocs ocs;

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

        ocs = new Ocs();
        ocs.setNome("Laboratorio Exemplo");
        ocs.setContratoNum("EXEMPLO-001");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.saveAndFlush(ocs);

        ProcedimentoExame hemograma = procedimento("40304361", "Hemograma com contagem de plaquetas");
        ProcedimentoExame glicose = procedimento("40302040", "Glicose");

        idEmAtencao = preGuia(medico, PreGuiaStatus.ATENCAO);
        vincular(idEmAtencao, hemograma);
        vincular(idEmAtencao, glicose);
        historico(idEmAtencao, "Primeira observacao", LocalDateTime.of(2026, 10, 11, 9, 0));
        historico(idEmAtencao, "Exame repetido, revise a lista", LocalDateTime.of(2026, 10, 15, 9, 0));

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
        p.setOcs(ocs);
        p.setMedicoFusex(criador);
        p.setStatus(status);
        if (criador == null) {
            p.setEncaminhamentoUrl("encaminhamentos/teste.pdf"); // rota do beneficiario
        }
        return preGuiaRepository.saveAndFlush(p).getIdPreGuia();
    }

    private void vincular(Long idPreGuia, ProcedimentoExame procedimento) {
        PreGuia preGuia = preGuiaRepository.getReferenceById(idPreGuia);
        preGuiaProcedimentoRepository.saveAndFlush(new PreGuiaProcedimento(
                new PreGuiaProcedimentoId(idPreGuia, procedimento.getIdProcedimentoExame()), preGuia, procedimento));
    }

    private void historico(Long idPreGuia, String observacao, LocalDateTime dataHora) {
        historicoRepository.saveAndFlush(new PreGuiaHistorico(
                null, preGuiaRepository.getReferenceById(idPreGuia), PreGuiaStatus.ATENCAO,
                observacao, null, setorGuias, dataHora));
    }

    private ResultActions buscar(String login, Long id) throws Exception {
        return mockMvc.perform(get("/medicos/me/pre-guias/" + id + "/correcao")
                .header(SimulacaoLoginFilter.HEADER, login));
    }

    @Test
    void sucesso_devolveOsDadosDaTelaDeCorrecao() throws Exception {
        int idade = beneficiario.idadeEm(Beneficiario.hoje());

        buscar(MEDICO, idEmAtencao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(String.format("%04d", idEmAtencao)))
                .andExpect(jsonPath("$.status").value("ATENCAO"))
                .andExpect(jsonPath("$.beneficiario.nome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$.beneficiario.precCp").value("45872213300"))
                .andExpect(jsonPath("$.beneficiario.faixaEtaria").value(FaixaEtaria.de(idade).getRotulo()))
                .andExpect(jsonPath("$.beneficiario.idade").value(idade))
                .andExpect(jsonPath("$.beneficiario.telefone").value("12987654321"))
                .andExpect(jsonPath("$.ocsAtual.id").value(ocs.getIdOcs()))
                .andExpect(jsonPath("$.ocsAtual.nome").value("Laboratorio Exemplo"))
                .andExpect(jsonPath("$.ocsAtual.contrato").value("EXEMPLO-001"))
                .andExpect(jsonPath("$.ocsAtual.tipo").value("OCS"))
                // em ordem alfabetica pelo nome
                .andExpect(jsonPath("$.procedimentos", hasSize(2)))
                .andExpect(jsonPath("$.procedimentos[0].codigoTuss").value("40302040"))
                .andExpect(jsonPath("$.procedimentos[0].nome").value("Glicose"))
                .andExpect(jsonPath("$.procedimentos[1].codigoTuss").value("40304361"))
                // a observacao mais recente do setor
                .andExpect(jsonPath("$.observacao.texto").value("Exame repetido, revise a lista"))
                .andExpect(jsonPath("$.observacao.dataRegistro").value("2026-10-15T09:00:00"));
    }

    @Test
    void deOutroMedico_retorna404() throws Exception {
        buscar(MEDICO, idDeOutroMedico).andExpect(status().isNotFound());
    }

    @Test
    void criadaPorBeneficiario_retorna403() throws Exception {
        buscar(MEDICO, idDoBeneficiario).andExpect(status().isForbidden());
    }

    @Test
    void statusDiferenteDeAtencao_retorna409() throws Exception {
        buscar(MEDICO, idPendente).andExpect(status().isConflict());
    }

    @Test
    void inexistente_retorna404() throws Exception {
        buscar(MEDICO, 999999L).andExpect(status().isNotFound());
    }

    @Test
    void perfilErrado_retorna403() throws Exception {
        buscar(BENEFICIARIO, idEmAtencao).andExpect(status().isForbidden());
    }

    @Test
    void semLogin_retorna401() throws Exception {
        mockMvc.perform(get("/medicos/me/pre-guias/" + idEmAtencao + "/correcao"))
                .andExpect(status().isUnauthorized());
    }
}
